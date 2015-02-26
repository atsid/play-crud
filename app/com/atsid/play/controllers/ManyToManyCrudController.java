package com.atsid.play.controllers;

import com.atsid.play.common.EbeanUtil;
import com.atsid.play.models.AssociationFinder;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import play.Logger;
import play.Play;
import play.db.ebean.Model;
import play.libs.F.Promise;
import play.libs.F.Function0;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* @author: alikalarsen
* Date: 7/1/13
*
* TODS (X=done,O=not done):
*
* X G list
* X   - fields, fetches, order, off, count, query
* X create: assoc & create, always append
* O   - batch
* X U update: update obj, support single and multiple ?
* O   - batch?
* X G read
* X   - fields, fetches
* X D delete: remove assoc, optionally delete object
* O   - batch?
*/
public class ManyToManyCrudController<L extends Model, J extends Model, R extends Model> extends CrudController<R> {

    private Class<L> leftClass;
    private String leftFieldName;
    private Class<J> junctionClass;
    private Class<R> rightClass;
    private String rightFieldName;

    public ManyToManyCrudController(Class<L> leftClass, Class<J> junctionClass, Class<R> rightClass) {
        this(leftClass, junctionClass, rightClass, leftClass.getSimpleName().toLowerCase(), rightClass.getSimpleName().toLowerCase());
    }

    public ManyToManyCrudController(Class<L> leftClass, Class<J> junctionClass, Class<R> rightClass, String leftFieldName, String rightFieldName) {
        super(rightClass);
        this.rightFieldName = rightFieldName;
        this.leftClass = leftClass;
        this.leftFieldName = leftFieldName;
        this.junctionClass = junctionClass;
        this.rightClass = rightClass;
    }

    /**
     * Gets a list of associations for the base model object
     * @return
     */
    @Override
    public Promise<Result> associations(Long leftId) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                List<Class<? extends Model>> associations = new ArrayList<Class<? extends Model>>();
                associations.addAll(AssociationFinder.findClassAssociations(junctionClass));
                List<String> simpleAssociations = new ArrayList<String>();
                for (Class<? extends Model> assoc : associations) {
                    simpleAssociations.add(assoc.getSimpleName());
                }
                return CrudResults.successCount(simpleAssociations.size(), simpleAssociations.size(), simpleAssociations);
            }

        });
    }

    /**
     * Gets a list of associations for the base model object
     * @return
     */
    public Promise<Result> associations(Long leftId, Long rightId) {
        return associations(leftId);
    }

    /**
     * Action
     * Query a list of models.
     * @param leftId The id of the parent model
     * @param offset An offset from the first item to start filtering from.  Used for paging.
     * @param count The total count to query.  This is the length of items to query after the offset.  Used for paging.
     * @param orderBy Order the queried models in order by the given properties of the model.
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param queryString Filter the models with a comma-delimited query string in the format of "property:value".
     * @return A promise containing the results
     */
    public Promise<Result> list(final Long leftId,final Integer offset, final Integer count, final String orderBy, final String fields, final String fetches, final String queryString) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                L left  = createLeftQuery(new ServiceParams()).where().eq("id", leftId).findUnique();
                if (left == null) {
                    return CrudResults.notFoundError(leftClass, leftId);
                }

                ServiceParams rightParams = new ServiceParams(offset, count, orderBy, fields, fetches, queryString);
                ServiceParams params = new ServiceParams(offset, count, orderBy, fields, fetches, queryString,  rightFieldName + ".");

                Query<J> query = createJunctionQuery(params);

                // turn query into like statements, with everything or'd
                // ?q=name:%bri%,location:seattle%
                if (queryString != null) {
                    handleSearchQuery(query, getJunctionClass(), params);
                }

                String appendedOrderBy = "";
                // ?orderBy=name asc
                if (orderBy != null && !orderBy.isEmpty()) {
                    appendedOrderBy = formatOrderBy(params.orderBy, junctionClass);
                    query.orderBy(appendedOrderBy);
                }

                handleFieldsAndFetches(query, junctionClass, params);

                query.where().eq(getTableName(leftClass, leftFieldName) + "_id", leftId);

                query.setFirstRow(offset);
                Integer actualCount = count == null ? Play.application().configuration().getInt("crud.defaultCount") : count;
                query.setMaxRows(actualCount == null ? 100 : count);

                if (count == null) {
                    Logger.warn("No count specified on request: " + request().uri());
                }

                List<J> junctions = junctions = query.findList();
                return CrudResults.successCount(
                    query.findRowCount(),
                    junctions.size(),
                    getChildObjects(junctions, rightParams));
            }
        });
    }

    /**
     * Create a new entity with the json body of the request.
     * If the body is an array, it will bulk create.
     * @param leftId The id of the parent object
     * @return Result object with the created entities
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> create(final Long leftId) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                L left  = createLeftQuery(new ServiceParams()).where().eq("id", leftId).findUnique();
                if (left == null) {
                    return CrudResults.notFoundError(leftClass, leftId);
                }

                List<R> rightList = new ArrayList<R>();
                List<J> junctionList = new ArrayList<J>();

                // if id is sent get existing model, otherwise create it
                JsonNode reqBody = request().body().asJson();
                JsonNode array;

                // convert the request into an array if it's not. Reduces logic.
                if (!reqBody.isArray()) {
                    ArrayNode a = new ArrayNode(JsonNodeFactory.instance);
                    a.add(reqBody);
                    array = a;
                } else {
                    array = reqBody;
                }

                for (final JsonNode node : array) {
                    JsonNode rightNode = node;
                    R right;
                    if (rightNode != null) {
                        JsonNode idNode = rightNode.get("id");
                        if (idNode != null && !idNode.asText().equals("null")) {
                            Long rightId = idNode.asLong(); //TODO: Slow
                            right = createQuery(new ServiceParams()).where().eq("id", rightId).findUnique();
                            if (right != null) {
                                rightList.add(right);
                            } else {
                                return CrudResults.notFoundError(getBaseModelClass(), rightId);
                            }
                        } else {
                            right = Json.fromJson(rightNode, getBaseModelClass());
                            ResultOrValue<R> rov = isModelValid(right);
                            if (rov.result != null) {
                                return rov.result;
                            } else {
                                rightList.add(right);
                            }
                        }
                    } else {
                        return CrudResults.error("No right object found on junction.");
                    }
                }

                for (R right : rightList) {
                    // save junction - this can be cleaned up
                    J junction = null;
                    try {
                        Constructor<J> ctor = junctionClass.getConstructor();
                        junction = ctor.newInstance();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    setSubObject(junction, left, leftFieldName);
                    setSubObject(junction, right, rightFieldName);
                    junctionList.add(junction);
                }

                Ebean.save(rightList);
                Ebean.save(junctionList);

                // return an array if the request was originally an array
                List<? extends Model> returnList = rightList;
                if (reqBody.isArray()) {
                    return CrudResults.successCreate(returnList);
                } else {
                    return CrudResults.successCreate(returnList.get(0));
                }
            }
        });
    }

    /**
     * Action
     * Read a single model.
     * @param leftId The id of the parent model.
     * @param rightId The id of the child model.
     * @param fields Fields to retrieve off the model. If not provided, retrieves all.
     * @param fetches Get any 1to1 relationships off the model. By default, it returns only the id off the relationships.
     * @return A promise containing the results
     */
    public Promise<Result> read(final Long leftId, final Long rightId, final String fields, final String fetches) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                L left  = createLeftQuery(new ServiceParams()).where().eq("id", leftId).findUnique();
                if (left == null) {
                    return CrudResults.notFoundError(leftClass, leftId);
                }

                ServiceParams rightParams = new ServiceParams(fields, fetches);
                ServiceParams params = new ServiceParams(fields, fetches, rightFieldName + ".");
                Query<J> query = createJunctionQuery(params);

                handleFieldsAndFetches(query, junctionClass, params);
                List<J> junctions = applyJunctionFiltering(leftId, rightId, query);
                List<R> rights = getChildObjects(junctions, rightParams);

                if (rights.size() == 0) {
                    return CrudResults.notFoundError(getBaseModelClass(), rightId);
                }

                List<? extends Model> returnList = rights;
                return CrudResults.success(returnList.get(0));
            }
        });
    }

    /**
     * Implements a update method
     * @param leftId The id of the parent model
     * @param rightId The id of the child model
     * @return
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> update(final Long leftId, final Long rightId) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                J model =
                    createJunctionQuery(new ServiceParams())
                        .where()
                            .eq(getTableName(rightClass, rightFieldName) + "_id", rightId)
                            .eq(getTableName(leftClass, leftFieldName) + "_id", leftId)
                        .findUnique();

                // You can only update rights that we have a junction for
                if (model == null) {
                    return CrudResults.notFoundError(junctionClass, "");
                }

                return ManyToManyCrudController.super.update(rightId).get(5000);
            }
        });
    }

    /**
     * Implements a delete method
     * @param leftId The id of the parent model
     * @param rightId The id of the child model
     * @return
     */
    public Promise<Result> delete(final Long leftId, final Long rightId) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                L left  = createLeftQuery(new ServiceParams()).where().eq("id", leftId).findUnique();
                if (left == null) {
                    return CrudResults.notFoundError(leftClass, leftId);
                }

                List<? extends Model> junctions;

                junctions = applyJunctionFiltering(leftId, rightId, null);

                List<? extends Model> rights = getChildObjects(junctions, new ServiceParams());
                List<? extends Model> toDelete = junctions;

                if (rights.size() == 0) {
                    return CrudResults.notFoundError(getBaseModelClass(), rightId);
                }
                // Can we not just do, createJunctionQuery().eq(leftId).eq(rightId).findList()?

                for (Model m : toDelete) {
                    m.delete();
                }
                return noContent();
            }
        });
    }

    /**
     * Implements a bulk delete method
     * @param leftId The id of the parent object
     * @return
     */
    @play.db.ebean.Transactional
    public Promise<Result> deleteBulk(final Long leftId) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                L left = createLeftQuery(new ServiceParams()).where().eq("id", leftId).findUnique();
                if (left == null) {
                    return CrudResults.notFoundError(leftClass, leftId);
                }

                List<Long> items = getItemsFromRequest();

                if (items != null) {
                    // TODO: Ebean doesn't support deletes based off a where clause.
                    // We should switch this to raw sql to reduce it to 1 db call instead of 2. V
                    List<J> junks = applyJunctionFiltering(leftId, items, null);
                    if (junks.size() == items.size()) {
                        //modelMediator.beforeModelDelete(junks);
                        Ebean.delete(junks);
                        //modelMediator.afterModelDelete(junks);
                        return noContent();
                    } else {
                        return CrudResults.notFoundError(junctionClass, "");
                    }
                } else {
                    return CrudResults.error("Array of objects with ids required");
                }
            }
        });
    }

    /**
     * Gets the parent class in this m2m controller
     * @return
     */
    protected Class<L> getParentClass() {
        return this.leftClass;
    }

    /**
     * Gets the junction class in this m2m controller
     * @return
     */
    protected Class<J> getJunctionClass() {
        return junctionClass;
    }

    /**
     * Returns the class to use when doing schema validation of the fields/fetches/orderby/querystring
     * @return
     */
    @Override
    protected Class getSchemaModel() {
        return this.junctionClass;
    }

    /**
     * Creates a query for the junction class
     * @param params The parameters passed to the services
     * @return
     */
    protected Query<J> createJunctionQuery(ServiceParams params) {
        return Ebean.createQuery(junctionClass).fetch(rightFieldName);
    }

    /**
     * Creates a query for the left (or parent) class
     * @param params The parameters passed to the services
     * @return
     */
    protected Query<L> createLeftQuery(ServiceParams params) {
        return Ebean.createQuery(leftClass);
    }

    /**
     * Filters a query to return only the junctions that point to the left model and one of the right models
     * @param leftId The id of the left item
     * @param rightItems The list of ids for the right items
     * @param query The query to filter
     * @return
     */
    private List<J> applyJunctionFiltering(Long leftId, List<Long> rightItems, Query<J> query) {
        if (query == null) {
            query = createJunctionQuery(new ServiceParams());
        }

        return query.where()
                .eq(getTableName(leftClass, leftFieldName) + "_id", leftId)
                .in(getTableName(junctionClass, rightFieldName) + "_id", rightItems)
                .findList();
    }

    /**
     * Filters a query to return only the junctions that point to the left model and the right model
     * @param leftId The id of the left item
     * @param rightId The id of the right item
     * @param query The query to filter
     * @return
     */
    private List<J> applyJunctionFiltering(Long leftId, Long rightId, Query<J> query) {
        if (query == null) {
            query = createJunctionQuery(new ServiceParams());
        }
        return query.where()
                .eq(getTableName(leftClass, leftFieldName) + "_id", leftId)
                .eq(getTableName(junctionClass, rightFieldName) + "_id", rightId)
                .findList();
    }

    /**
     * Sets either the child model or the parent model on the junction model
     * @param junction The junction to set the property on
     * @param model The model to set
     * @param fieldName The parent or child field name
     */
    private void setSubObject(J junction, Object model, String fieldName) {
        List<String> fields = Arrays.asList(junction._ebean_getFieldNames());
        int fieldIndex = fields.indexOf(fieldName);
        junction._ebean_setField(fieldIndex, junction, model);
    }

    /**
     * Retrieves a list of child objects for the given junctions
     * @param junctions The junctions to get the child objects from
     * @param params The params passed to the services
     * @return A list of child objects
     */
    private List<R> getChildObjects(List<? extends Model> junctions, ServiceParams params) {
        List ids = new ArrayList();
        for (Model junction : junctions) {
            List<String> fields = Arrays.asList(junction._ebean_getFieldNames());
            int fieldIndex = fields.indexOf(rightFieldName);
            ids.add(EbeanUtil.getFieldValue((R) junction._ebean_getField(fieldIndex, junction), "id"));
        }
        Query<R> query = createQuery(params);
        handleFieldsAndFetches(query, rightClass, params);

        // ?orderBy=name asc
        if (params.orderBy != null && params.orderBy.length > 0) {
            query.orderBy(formatOrderBy(params.orderBy, rightClass));
        }

        return query.where().in("id", ids).findList();
    }

    /**
     * Converts a field name to a table name.
     * example: aFieldName -> a_field_name
     * @param fieldName
     * @return The table name.
     */
    private String getTableName(java.lang.Class<?>modelClass, String fieldName) {
        UnderscoreNamingConvention nameConvention = new UnderscoreNamingConvention();
        return nameConvention.getColumnFromProperty(modelClass, fieldName);
    }
}
