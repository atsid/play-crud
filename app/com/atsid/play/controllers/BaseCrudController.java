package com.atsid.play.controllers;

import com.atsid.play.common.exceptions.InvalidFieldException;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.Junction;
import com.avaje.ebean.Query;
import com.avaje.ebean.bean.EntityBean;
import com.atsid.play.common.EbeanUtil;
import com.atsid.play.common.SchemaBuilder;
import com.atsid.play.models.AssociationFinder;
import com.atsid.play.models.schema.FieldDescriptor;
import com.atsid.play.models.schema.FieldType;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.Play;
import play.data.Form;
import play.db.ebean.Model;
import play.libs.F.Promise;
import play.libs.F.Function0;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseCrudController<I, T extends Model> extends Controller {
    private final Class<T> modelClass;
    private final Class<I> idClass;
    private Boolean validate = true;

    public BaseCrudController(Class<I> idType, Class<T> clazz) {
        modelClass = clazz;
        idClass = idType;
    }

    protected Class<T> getBaseModelClass() {
        return modelClass;
    }

    /**
     * Create a new entity with the json body of the request.
     * If the body is an array, it will bulk create.
     * @return Result object with the created entities
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> create() {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                JsonNode json = request().body().asJson();

                if (json.isArray()) {
                        ResultOrValue<List<T>> modelList = createModelListFromJson(json);
                        if (modelList.result == null) {
                            return create(modelList.value);
                        }
                        return modelList.result;
                }
                ResultOrValue<T> model = createModelFromJson(json);
                if (model.result == null) {
                    return create(model.value);
                }
                return model.result;
            }

        });
    }

    /**
     * Create a new model entity
     * @param model The new model
     * @return A JSON result
     */
    public Result create(final T model) {
        ResultOrValue<T> rov = isModelValid(model);
        if (rov.result != null) {
            return rov.result;
        }
        Result r = CrudResults.successCreate(this.saveModel(model));
        return r;
    }

    /**
     * Create a list of new model entities
     * @param modelList A list of new models.
     * @return A JSON result
     */
	public Result create(final List<T> modelList) {
        for (T model : modelList) {
            ResultOrValue<T> rov = isModelValid(model);
            if (rov.result != null) {
                return rov.result;
            }
        }

        List<T> savedModels = saveModel(modelList);
        return CrudResults.successCreate(savedModels);
    }

    /**
     * Action
     * Query a list of models.
     * @param offset An offset from the first item to start filtering from.  Used for paging.
     * @param count The total count to query.  This is the length of items to query after the offset.  Used for paging.
     * @param orderBy Order the queried models in order by the given properties of the model.
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param queryString Filter the models with a comma-delimited query string in the format of "property:value".
     * @return A promise containing the results
     */
    public Promise<Result> list(final int offset, final Integer count, final String orderBy, final String fields, final String fetches, final String queryString) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                if (count == null) {
                    Logger.warn("No count specified on request: " + request().uri());
                }
                ServiceParams params = new ServiceParams(offset, count, orderBy, fields, fetches, queryString);
                Query<T> query = createQuery(params);
                updateQuery(query, params);

                List<T> modelList = queryList(query, params);

                return CrudResults.successCount(
                    query.findRowCount(),
                    modelList.size(),
                    modelList);
            }
        });
    }

    /**
     * Action
     * Read a single model.
     * @param id The id of the model.
     * @param fields Fields to retrieve off the model. If not provided, retrieves all.
     * @param fetches Get any 1to1 relationships off the model. By default, it returns only the id off the relationships.
     * @return A promise containing the results
     */
    public Promise<Result> read(final I id, final String fields, final String fetches) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                ServiceParams params = new ServiceParams(fields, fetches);
                Query<T> query = createQuery(params);
                T model = null;
                model = queryOne(query, id, params);

                if (model != null) {
                    return CrudResults.success(model);
                }
                return CrudResults.notFoundError(getBaseModelClass(), id);
            }
        });
    }

    /**
     * Action
     * Update a model.
     * @param id The id of the model
     * @return A promise containing the results
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> update(final I id) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                ResultOrValue<T> model = createModelFromJson(request().body().asJson());
                if (model.result == null) {
                    return update(id, model.value);
                }
                return model.result;
            }
        });
    }

    /**
    * Update a model.
    * @param id The id of the model to update.
    * @param model The model to update.
    * @return Json Result
    */
    public Result update(final I id, final T model) {
        ResultOrValue<T> rov = isModelValid(model);
        if (rov.result != null) {
            return rov.result;
        }

        // Make sure it actually exists in the DB
        T dbModel = Ebean.createQuery(getBaseModelClass()).where().idEq(id).findUnique();
        if (dbModel == null) {
            return CrudResults.notFoundError(getBaseModelClass(), id);
        }
        updateModel(model);

        T updated = Ebean.createQuery(getBaseModelClass()).where().idEq(id).findUnique();
        JsonNode json = Util.toJson(updated, true);
        return CrudResults.ok(json);
    }

    /**
     * Action
     * Update a list of models
     * @return A promise containing the results
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> updateBulk() {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                ResultOrValue<List<T>> modelList = createModelListFromJson(request().body().asJson());
                if (modelList.result == null) {
                    return updateBulk(modelList.value);
                }
                return modelList.result;
            }
        });
    }

    public Result updateBulk(final List<T> modelList) {
        List<I> ids = new ArrayList<I>();
        ResultOrValue<T> rov = null;
        for (T model : modelList) {
            rov = isModelValid(model);
            if (rov.result != null) {
                return rov.result;
            } else {
                ids.add((I)EbeanUtil.getFieldValue(model, "id"));
            }
        }

        // Verify they exist in the DB
        List<T> dbModels = createQuery(new ServiceParams()).select("id").where().in("id", ids).findList();

        // If we don't have the same number of DB results as ids, then at least one of them is missing
        if (dbModels.size() != ids.size()) {
            return CrudResults.notFoundError(this.modelClass, "");
        }

        // FIXME: This should follow the same format as other results.  Left like this for compatibility.
        return play.mvc.Results.ok(Util.toJson(updateModel(modelList)));
    }

    /**
     * Delete a model
     * @param id The id of the model
     * @return A promise containing the results.  Returns no content.
     */
    public Promise<Result> delete(final I id) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                T model = createQuery(new ServiceParams()).where().eq("id", id).findUnique();
                if (model == null) {
                    return CrudResults.notFoundError(getBaseModelClass(), id);
                }
                return delete(model);
            }
        });
    }

    /**
     * Delete a model
     * @param model The model to delete.
     * @return A promise containing the results.  Returns no content.
     */
    public Result delete(T model) {
        model.delete();
        return play.mvc.Results.noContent();
    }

    /**
     * Bulk delete models.  The body should be an array of objects with an id.
     * @return A promise containing the results.  Returns no content.
     */
    @play.db.ebean.Transactional
    @BodyParser.Of(BodyParser.Json.class)
    public Promise<Result> deleteBulk() {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                List<I> items = getItemsFromRequest();
                if (items != null) {
                    List<T> models = createQuery(new ServiceParams()).where().in("id", items).findList();

                    // Some of the items are missing
                    if (models.size() != items.size()) {
                        return CrudResults.notFoundError(modelClass, "");
                    }

                    // TODO: Try to get Ebean.delete() working properly.
                    for (T t : models) {
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
     * @param id The id of the model.
     * @return A promise containing the results.
     */
    public Promise<Result> associations(I id) {
        return Promise.promise(new Function0<Result>() {
            public Result apply() {
                List<Class<? extends Model>> associations = AssociationFinder.findClassAssociations(getBaseModelClass());
                List<String> simpleAssociations = new ArrayList<String>();
                for (Class<? extends Model> assoc : associations) {
                    simpleAssociations.add(assoc.getSimpleName());
                }
                return CrudResults.successCount(simpleAssociations.size(), simpleAssociations.size(), simpleAssociations);
            }
        });
    }

    public ResultOrValue<T> createModelFromJson(JsonNode json) {
        T model = Json.fromJson(json, getBaseModelClass());
        if (model != null) {
            return new ResultOrValue<T>(model);
        }
        return new ResultOrValue<T>(CrudResults.badRequest("Invalid json for object."));
    }

    public ResultOrValue<List<T>> createModelListFromJson(JsonNode jsonArray) {
        if (jsonArray == null || !jsonArray.isArray()) {
            return new ResultOrValue<List<T>>(CrudResults.error("Array of objects with ids required"));
        }

        List<T> modelList = new ArrayList<T>();
        Integer index = 0;
        for (final JsonNode modelNode : jsonArray) {
            ResultOrValue<T> model = createModelFromJson(modelNode);
            if (model.result != null) {
                return new ResultOrValue<List<T>>(CrudResults.badRequest(String.format("Invalid json for object at index %d", index)));
            }
            index += 1;
            modelList.add(model.value);
        }
        return new ResultOrValue<List<T>>(modelList);
    }

    /**
     * Create a new model.
     * @param model
     * @return
     */
    public T saveModel(T model) {
        //model.refresh(); //TODO Test to see if this is needed.
        Ebean.save(model);
        return model;
    }

    public List<T> saveModel(List<T> modelList) {
        Ebean.save(modelList);
        return modelList;
    }

    protected T updateModel(T model) {
        Ebean.update(model);
        //EntityBean ref = Ebean.getReference(getBaseModelClass(), EbeanUtil.getFieldValue(model, "id"));
        return model;
    }

    @play.db.ebean.Transactional
    protected List<T> updateModel(List<T> modelList) {
        List<EntityBean> refs = new ArrayList<EntityBean>();
        for (final T model : modelList) {
            model.update();
            refs.add(Ebean.getReference(getBaseModelClass(), EbeanUtil.getFieldValue(model, "id")));
        }
        return modelList;
    }

    protected Query<T> createQuery(ServiceParams params) {
        return createRawQuery(params);
    }

    protected Query<T> createRawQuery(ServiceParams params) {
        return Ebean.createQuery(getBaseModelClass());
    }

    protected void updateQuery(Query<T> query, ServiceParams params) {
        // turn query into like statements, with everything or'd
        // ?q=name:%bri%,location:seattle%
        handleSearchQuery(query, params);

        // ?orderBy=name asc
        handleOrderBy(query, getSchemaModel(), params);

        handleFieldsAndFetches(query, params);
    }

    protected List<T> queryList(Query<T> query, ServiceParams params) {
        int offset = params.offset;
        Integer count = params.count;
        query.setFirstRow(offset);
        count = count == null ? Play.application().configuration().getInt("crud.defaultCount") : count;
        query.setMaxRows(count == null ? 100 : count);
        List<T> modelList = query.findList();
        return modelList;
    }

    protected T queryOne(Query<T> query, I id, ServiceParams params)  {
        handleFieldsAndFetches(query, getSchemaModel(), params);
        return query.where().idEq(id).findUnique();
    }

    /**
     * Handles the search query passed to the service
     * @param query The database query
     * @param params The service parameters
     */
    protected void handleSearchQuery(Query query, ServiceParams params) {
        handleSearchQuery(query, getSchemaModel(), params);
    }

    /**
     * Handles the search query passed to the service
     * @param query The database query
     * @param params The service parameters
     */
    protected void handleSearchQuery(Query query, Class modelClass, ServiceParams params) {
        if (params.queryString != null && !params.queryString.isEmpty()) {
            String queryString = params.queryString;
            Matcher matcher = Pattern.compile("AND|OR").matcher(queryString);
            ExpressionList list = query.where();
            if (matcher.find()) {
                int lastIndex = 0;
                boolean lastIsOr = false;
                boolean isOr;
                Junction junction = null;
                do {
                    String currentField = queryString.substring(lastIndex, matcher.start());
                    lastIndex = matcher.end();
                    isOr = matcher.group(0).equals("OR");
                    if (junction == null || lastIsOr != isOr) {
                        if (junction != null) {
                            list = junction.endJunction();
                        }
                        junction = isOr ? list.disjunction() : list.conjunction();
                        list = junction;
                    }
                    junction.add(getSearchExpression(currentField, modelClass));
                    lastIsOr = isOr;
                } while (matcher.find());
                junction.add(getSearchExpression(queryString.substring(lastIndex), modelClass));
                junction.endJunction();
            } else {
                Expression ex = getSearchExpression(queryString, modelClass);
                if (ex != null) {
                    list.add(ex);
                }
            }
        }
    }

    /**
     * Creates a search expression from a string
     * @param fieldQuery The field query string
     * @param modelClass The class of the model to search
     * @return
     */
    protected Expression getSearchExpression(String fieldQuery, Class modelClass) {
        int colonIndex = fieldQuery.indexOf(":");
        boolean negate = false;
        Expression e = null;
        if (colonIndex > -1) {
            String finalField = fieldQuery.substring(0, colonIndex);
            String value = fieldQuery.substring(colonIndex + 1);
            if (value.startsWith("!")) {
                value = value.substring(1);
                negate = true;
            }

            // TODO: Wont work so well on nested props
            FieldDescriptor descriptor = SchemaBuilder.lookupFieldDescriptor(modelClass, finalField);
            if (descriptor != null) {
                FieldType type = descriptor.type;

                if (type == FieldType.DATE) {
                    finalField = Util.isUsingH2Database() ? "CAST(" + finalField + " as DATE)" : "DATE(" + finalField + ")";
                    value = value.replaceFirst("[T].*", "");
                }

                if (value.startsWith(">=")) {
                    value = value.substring(2);
                    e = Expr.ge(finalField, value);
                } else if (value.startsWith("<=")) {
                    value = value.substring(2);
                    e = Expr.le(finalField, value);
                } else if (value.startsWith(">")) {
                    value = value.substring(1);
                    e = Expr.gt(finalField, value);
                } else if (value.startsWith("<")) {
                    value = value.substring(1);
                    e = Expr.lt(finalField, value);
                } else {
                    if (type != FieldType.STRING) {
                        if (type == FieldType.BOOLEAN) {
                            // Convert it to boolean, so ebean can handle it appropriately
                            e = Expr.eq(finalField, value.equalsIgnoreCase("true"));
                        } else if (negate) {
                            // Include null / empty values. Doesn't seem to work with Expr.not(), so it returns directly.
                            return Expr.or(Expr.isNull(finalField), Expr.ne(finalField, value));
                        } else {
                            e = Expr.eq(finalField, value);
                        }
                    } else {
                        e = Expr.ilike(finalField, value);
                    }
                }
                if (negate) {
                    e = Expr.not(e);
                }
            } else {
                throw new InvalidFieldException(finalField);
            }
        }
        return e;
    }

    /**
     * Handles the order by passed to the service
     * @param query
     * @param params The service parameters
     * @param modelClass
     */
    protected void handleOrderBy(Query query, Class modelClass, ServiceParams params) {
        if (params.orderBy != null && params.orderBy.length > 0) {
            query.orderBy(formatOrderBy(params.orderBy, modelClass));
        }
    }

    /**
     * Formats the order by string passed to the service
     * @param orderBy
     * @param parentClass
     * @return
     */
    protected String formatOrderBy(String[] orderBy, Class parentClass) {
//        String[] fields = orderBy.split(","); // csv of orderBy's
        List<String> sorts = new ArrayList<String>();
        for (String f : orderBy) {
            String[] split = f.trim().split(" "); // split by space
            FieldDescriptor desc = SchemaBuilder.lookupFieldDescriptor(parentClass, split[0]);
            if (desc != null) {
                // LOWER it if it's a string
                if (desc.type == FieldType.STRING) {
                    sorts.add("LOWER(" + split[0] + ") " + (split.length > 1 ? split[1] : "asc"));
                } else {
                    sorts.add(f);
                }
            } else {
                throw new InvalidFieldException(split[0]);
            }

        }

        return StringUtils.join(sorts, ',');
    }

    /**
     * Takes care of adding fields/fetches to the query
     * @param query Query to modify
     * @param params The service parameters
     */
    protected void handleFieldsAndFetches(Query query, ServiceParams params)  {
        this.handleFieldsAndFetches(query, getSchemaModel(), params);
    }

    /**
     * Takes care of adding fields/fetches to the query
     * @param query Query to modify
     * @param params The service parameters
     */
    protected void handleFieldsAndFetches(Query query, Class queryModelClass, ServiceParams params)  {
        handleFields(query, queryModelClass, params);
        handleFetches(query, queryModelClass, params);
    }
//
//    /**
//     * Takes care of adding fields/fetches to the query
//     * @param query Query to modify
//     * @param params The service parameters
//     */
//    protected void handleFieldsAndFetches(Query query, Class modelClass, ServiceParams params, String prepend) {
//        handleFields(query, modelClass, params, prepend);
//        handleFetches(query, modelClass, params, prepend);
//    }

    /**
     * Takes care of adding fields to the query.
     * @param query Query to modify
     * @param params The service parameters
     */
    protected void handleFields(Query query, Class modelClass, ServiceParams params) {
        if (params.fields != null && !params.fields.isEmpty()) {
            String[] fieldsArray = params.fields.toArray(new String[0]);
            // remove fields that contain periods, they'll get used by fetches
            String rootFields = "";
            for (String field : fieldsArray) {

                // Validate if valid field
                if (SchemaBuilder.lookupFieldDescriptor(modelClass, field) == null) {
                    throw new InvalidFieldException(field);
                }

                if (!field.contains(".")) {
                    if (rootFields.length() > 0) {
                        rootFields += ",";
                    }
                    rootFields += field;
                }
            }
            query.select(rootFields);
        }
    }

    /**
     * Fetches sub objects with support for partials if fields is not null.
     * Fetches is a comma separated list of sub objects on this model.
     * Examples:
     *      ?fetches=organization,organization.location
     *      ?fetches=organization&amp;fields=organization.name
     */
    protected void handleFetches(Query query, Class modelClass, ServiceParams params) {
        if (params.fetches != null && !params.fetches.isEmpty()) {
            String[] fetchAttrs = params.fetches.toArray(new String[0]);
            // no fields, just do the fetches
            if (params.fields == null) {
                for (String attr : fetchAttrs) {

                    // Validate if valid field
                    if (SchemaBuilder.lookupFieldDescriptor(modelClass, attr) == null) {
                        throw new InvalidFieldException(attr);
                    }

                    query.fetch(attr, new FetchConfig().query());
                }
            } else {
                // if there are fields, find the ones that are relevant to the sub objects
                for (String attr : fetchAttrs) {

                    // Validate if valid field
                    if (SchemaBuilder.lookupFieldDescriptor(modelClass, attr) == null) {
                        throw new InvalidFieldException(attr);
                    }

                    String[] fieldsArray = params.fields.toArray(new String[0]);
                    String fetchFields = "";
                    for (String field : fieldsArray) {

                        // Validate if valid field
                        if (SchemaBuilder.lookupFieldDescriptor(modelClass, field) == null) {
                            throw new InvalidFieldException(field);
                        }

                        String finalField = field.replace(attr + ".", "");
                        if (finalField.length() < field.length() && !finalField.contains(".")) {
                            if (fetchFields.length() > 0) {
                                fetchFields += ",";
                            }
                            fetchFields += finalField;
                        }
                    }
                    if (fetchFields.length() > 0) {
                        query.fetch(attr, fetchFields, new FetchConfig().query());
                    } else {
                        query.fetch(attr, new FetchConfig().query());
                    }
                }
            }
        }
    }

    /**
     * Returns true if the given field should show up in the schema
     */
    protected Boolean allowSchemaForField(String fieldName) {
        return true;
    }

    /**
     * Returns the model to use for schema validation
     */
    protected Class getSchemaModel() {
        return this.modelClass;
    }

    protected abstract List<I> getItemsFromRequest();

    public void setValidate (Boolean validate) {
        this.validate = validate;
    }

    public Boolean getValidate () {
        return this.validate;
    }

    protected ResultOrValue<T> isModelValid(T model) {
        if (validate == true) {
            Form<T> form = new Form<T>(getBaseModelClass()).bind(Util.toJson(model));
            if (form.hasErrors()) {
                return new ResultOrValue<T>(CrudResults.validationError(getBaseModelClass(), model));
            }
        }
        return new ResultOrValue<T>(model);
    }
}
