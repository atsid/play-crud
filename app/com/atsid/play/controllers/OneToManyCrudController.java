package com.atsid.play.controllers;

import com.atsid.play.common.EbeanUtil;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.BeanWrapperImpl;
import play.db.ebean.Model;
import play.libs.F.Promise;
import play.libs.F.Function0;
import play.mvc.BodyParser;
import play.mvc.Results;
import play.mvc.Result;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static play.libs.F.Tuple;

/**
 * @author: alikalarsen
 * Date: 7/1/13
*/
public class OneToManyCrudController<P extends Model, C extends Model> extends CrudController<C> {

    private Class<P> parentClass;
    private String parentFieldName;
    private Method parentSetter;

    public OneToManyCrudController(Class<P> parentClass, Class<C> childClass, String parentFieldName) {
        super(childClass);
        this.parentClass = parentClass;
        this.parentFieldName = parentFieldName;
    }

    /**
     * Action
     * Query a flat list of models without the parent
     * @param offset An offset from the first item to start filtering from.  Used for paging.
     * @param count The total count to query.  This is the length of items to query after the offset.  Used for paging.
     * @param orderBy Order the queried models in order by the given properties of the model.
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param queryString Filter the models with a comma-delimited query string in the format of "property:value".
     * @return A promise containing the results
     */
    public Promise<Result> listAll(final int offset, final Integer count, final String orderBy, final String fields, final String fetches, final String queryString) {
        return super.list(offset, count, orderBy, fields, fetches, queryString);
    }

    /**
     * Action
     * Query a flat list of models without the parent
     * @param offset An offset from the first item to start filtering from.  Used for paging.
     * @param count The total count to query.  This is the length of items to query after the offset.  Used for paging.
     * @param orderBy Order the queried models in order by the given properties of the model.
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param queryString Filter the models with a comma-delimited query string in the format of "property:value".
     * @return A promise containing the results
     */
    public Promise<Result> list(final Long pid, final Integer offset, final Integer count, final String orderBy, final String fields, final String fetches, final String queryString) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                P parent = createParentQuery(new ServiceParams()).where().eq("id", pid).findUnique();
                if (parent == null) {
                    return Results.notFound(pid.toString());
                }

                ServiceParams params = new ServiceParams(offset, count, orderBy, fields, fetches, queryString);
                Query<C> query = createQuery(params);
                updateQuery(query, params);

                // Restrict the query to the parent
                query.where().eq(parentFieldName + ".id", pid);

                List<C> modelList = queryList(query, params);

                return CrudResults.successCount(
                    query.findRowCount(),
                    modelList.size(),
                    modelList);
            }
        });
    }

    /**
     * Create a new entity with the json body of the request.
     * If the body is an array, it will bulk create.
     * @param pid The id of the parent object
     * @return Result object with the created entities
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> create(final Long pid) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                P parent = createParentQuery(new ServiceParams()).where().eq("id", pid).findUnique();
                if (parent == null) {
                    return Results.notFound(pid.toString());
                }

                JsonNode json = request().body().asJson();
                if (json.isArray()) {
                    ResultOrValue<List<C>> rov = createModelListFromJson(json);
                    List<C> children = rov.value;
                    if (children != null) {
                        for (C c : children) {
                            setParent(parent, c);
                        }

                        return create(children);
                    }
                    return rov.result;
                }
                    // create one, set parent
                ResultOrValue<C> rov = createModelFromJson(json);
                C model = rov.value;
                if (model != null) {
                    if (canAssignToParent(model, pid)) {
                        setParent(parent, model);
                        return create(model);
                    } else {
                        return CrudResults.notFoundError(getBaseModelClass(), EbeanUtil.getFieldValue(model, "id"));
                    }
                }
                return rov.result;
            }
        });
    }

    /**
     * Action
     * Read a single model.
     * @param parentId The id of the parent model.
     * @param fields Fields to retrieve off the model. If not provided, retrieves all.
     * @param fetches Get any 1to1 relationships off the model. By default, it returns only the id off the relationships.
     * @return A promise containing the results
     */
    public Promise<Result> read(final Long parentId, final Long id, final String fields, final String fetches) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                ServiceParams params = new ServiceParams(fields, fetches);
                Query<C> query = createQuery(params);
                handleFieldsAndFetches(query, getBaseModelClass(), params);

                // Tack on the parent
                query.where().eq("id", id).eq(parentFieldName + ".id", parentId);
                C model = query.findUnique();
                if (model != null) {
                    return CrudResults.success(model);
                }
                return CrudResults.notFoundError(getBaseModelClass(), id);
            }
        });
    }

    /**
     * Implements a update method
     * @param pid The id of the parent model
     * @param id The id of the child model
     * @return
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> update(final Long pid,final Long id) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                P parent = createParentQuery(new ServiceParams()).where().eq("id", pid).findUnique();
                if (parent == null) {
                    return Results.notFound(pid.toString());
                }

                JsonNode json = request().body().asJson();
                if (json.isArray()) {
                    ResultOrValue<List<C>> rov = createModelListFromJson(json);
                    List<C> children = rov.value;
                    if (children != null) {
                        if (canAssignToParent(children, pid)) {
                            for (C c : children) {
                                setParent(parent, c);
                            }
                            return updateBulk(children);
                        } else {
                            return CrudResults.notFoundError(getBaseModelClass(), "");
                        }
                    }
                    return rov.result;
                }
                // create one, set parent
                ResultOrValue<C> rov = createModelFromJson(json);
                C model = rov.value;
                if (model != null) {
                    if (canAssignToParent(model, pid)) {
                        setParent(parent, model);
                        return update(id, model);
                    } else {
                        return CrudResults.notFoundError(getBaseModelClass(), EbeanUtil.getFieldValue(model, "id"));
                    }
                }
                return rov.result;
            }
        });
    }

    /**
     * Action
     * Update a list of models
     * @param pid The parent id
     * @return A promise containing the results
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> updateBulk(final Long pid) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                P parent = createParentQuery(new ServiceParams()).where().eq("id", pid).findUnique();
                if (parent == null) {
                    return Results.notFound(pid.toString());
                }

                JsonNode json = request().body().asJson();
                if (json.isArray()) {
                    ResultOrValue<List<C>> rov = createModelListFromJson(json);
                    List<C> children = rov.value;
                    if (children != null) {
                        if (canAssignToParent(children, pid)) {
                            for (C c : children) {
                                setParent(parent, c);
                            }
                            return updateBulk(children);
                        } else {
                            return CrudResults.notFoundError(getBaseModelClass(), "");
                        }
                    }
                    return rov.result;
                } else {
                    return CrudResults.badRequest("Must be an array of items.");
                }
            }
        });
    }

    /**
     * Delete a model
     * @param parentId The id of the parent model
     * @param id The id of the child model
     * @return A promise containing the results.  Returns no content.
     */
    public Promise<Result> delete(Long parentId, Long id) {
        C dbModel = createQuery(new ServiceParams()).where().eq("id", id).eq(parentFieldName + ".id", parentId).findUnique();
        if (dbModel != null) {
            return super.delete(id);
        } else {
            final Long myId = id;
            return Promise.promise(new Function0<Result>() {
                public Result apply() {
                    return CrudResults.notFoundError(getBaseModelClass(), myId);
                }
            });
        }
    }

    /**
     * Bulk delete models.  The body should be an array of objects with an id.
     * @param parentId The id of the parent
     * @return A promise containing the results.  Returns no content.
     */
    @play.db.ebean.Transactional
    public Promise<Result> deleteBulk(Long parentId) {
        final Long myParent = parentId;
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                List<Long> items = getItemsFromRequest();
                if (items != null) {
                    List<C> models = createQuery(new ServiceParams()).where().in("id", items).eq(parentFieldName + ".id", myParent).findList();

                    // Some of the items are missing
                    if (models.size() != items.size()) {
                        return CrudResults.notFoundError(getBaseModelClass(), "");
                    }

                    // TODO: Try to get Ebean.delete() working properly.
                    for (C t : models) {
                        t.delete();
                    }
                    return noContent();
                }
                return CrudResults.error("Array of objects with ids required");
            }
        });
    }

    /**
     * Gets a list of associations for the base model object
     * @return
     */
    public Promise<Result> associations(Long pId, Long cId) {
        return super.associations(cId);
    }

    /**
     * Validates that the given child actually belongs to the parent in the db
     * @param child The child to validate
     * @return
     */
    public Boolean validateParent (C child) {
        P parent = (P) EbeanUtil.getFieldValue(child, parentFieldName);
        P dbParent = createParentQuery(new ServiceParams()).where().eq("id", EbeanUtil.getFieldValue(parent, "id")).findUnique();
        return (parent != null && dbParent != null);
    }

    /**
     * Gets the parent class
     * @return
     */
    protected Class<P> getParentClass() {
        return this.parentClass;
    }

    @Override
    /**
     * Validate a child's parent.
     */
    protected ResultOrValue<C> isModelValid(C model) {
        if (!validateParent(model)) {
            return new ResultOrValue<C>(CrudResults.methodNotAllowed("METHOD", parentClass.getName() + "." + getBaseModelClass().getName()));
        }
        return super.isModelValid(model);
    }

    /**
     * Creates a query for the parent model
     * @param params
     * @return
     */
    protected Query<P> createParentQuery(ServiceParams params) {
        return Ebean.createQuery(this.parentClass);
    }

    /**
     * Determines if the given child is a valid child of the given parent
     * @param child
     * @param pid
     * @return
     */
    private Boolean canAssignToParent(C child, Object pid) {
        // The user passed in an existing model as a child
        // Models without ids can already be assigned to the parent, cause they are new
        Long id = (Long)EbeanUtil.getFieldValue(child, "id");
        if (id != null) {
            // We cannot actually find the model in the DB
            C dbModel = createQuery(new ServiceParams()).where().eq("id", id).eq(parentFieldName + ".id", pid).findUnique();
            if (dbModel == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if the given children are valid children of the given parent
     * @param children
     * @param pid
     * @return
     */
    private Boolean canAssignToParent(List<C> children, Object pid) {
        List<Long> ids = new ArrayList<Long>();
        for (C child : children) {
            Long id = (Long)EbeanUtil.getFieldValue(child, "id");

            // Models without ids can already be assigned to the parent, cause they are new
            if (id != null) {
                ids.add(id);
            }
        }

        // Verify they exist in the DB
        List<C> dbModels = createQuery(new ServiceParams()).where().in("id", ids).eq(parentFieldName + ".id", pid).findList();

        // If we don't have the same number of DB results as ids, then one of them is missing
        if (dbModels.size() != ids.size()) {
            for (C dbModel : dbModels) {
                Long id = (Long)EbeanUtil.getFieldValue(dbModel, "id");
                if (!ids.contains(id)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets the parent setter method from the child class, and caches itroutes.
     * @return
     */
    private Method getParentSetter() {
        if (parentSetter != null) {
            return parentSetter;
        }
        try {
            Class<?> clazz = getBaseModelClass();
            Field parentField = clazz.getField(parentFieldName);
            PropertyDescriptor fieldDescriptor = new BeanWrapperImpl(clazz).getPropertyDescriptor(parentField.getName());
            parentSetter = fieldDescriptor.getWriteMethod();
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return parentSetter;
    }

    /**
     * Sets the given parent on the child
     * @param parent The parent model
     * @param child The child model
     */
    private void setParent(P parent, C child) {
        Method parentSetter = getParentSetter();
        try {
            parentSetter.invoke(child, parent);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
