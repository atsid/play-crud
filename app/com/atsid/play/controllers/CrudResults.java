package com.atsid.play.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.api.mvc.Codec;
import play.core.j.JavaResults;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A collection of Result types used by the crud controllers
 *  TODO: inject secruity provider
 */
public class CrudResults extends Results {

    /**
     * CrudResults specific results
     */

    /**
     * Returns a Result with a success message, and a OK status
     * @param message The message
     * @return The result
     */
    public static Result successMessage(String message) {
        ObjectNode result = Json.newObject();
        result.put("status", "success");
        result.put("message", message);
        return Results.ok(result).as("application/json");
    }

    /**
     * Returns a Result with a json payload, and a OK status
     * @param data The object to convert to json
     * @return The result
     */
    public static Result success(Object data) {
        return successJson(Util.toJson(data));
    }

    /**
     * Returns a Result with a json payload, and a OK status
     * @param node The object to convert to json
     * @return The result
     */
    public static Result successJson(JsonNode node) {
        ObjectNode result = Json.newObject();
        result.put("status", "success");
        result.put("data", node);
        return Results.ok(result).as("application/json");
    }

    /**
     * Returns a Result with a json payload, and a CREATED status
     * @param data The object to convert to json
     * @return The result
     */
    public static Result successCreate(Object data) {
        ObjectNode result = Json.newObject();
        result.put("status", "success");
        result.put("data", Util.toJson(data, true));
        return Results.created(result).as("application/json");
    }

    /**
     * Returns a Result with a json array payload (used for list base results), and a OK status
     * @param data The object to convert to json, should be an error
     * @return The result
     */
    public static Result successCount(int total, int count, Object data) {
        ObjectNode result = Json.newObject();
        result.put("total", total);
        result.put("status", "success");
        result.put("count", count);
        result.put("data", Util.toJson(data));
        return Results.ok(result).as("application/json");
    }

    /**
     * Returns a Result with an error message, and a BAD_REQUEST status
     * @param message The error message
     * @return
     */
    public static Result error(String message) {
        ObjectNode result = Json.newObject();
        result.put("status", "error");
        result.put("data", message);
        return Results.badRequest(result).as("application/json");
    }

    /**
     * Returns a Result with an error message, and a INTERNAL_SERVICE_ERROR status
     * @param message The error message
     * @return
     */
    public static Result unknownError(String message) {
        ObjectNode result = Json.newObject();
        result.put("status", "error");
        result.put("data", message);
        return Results.internalServerError(result).as("application/json");
    }

    /**
     * Returns a Result with an error message, and a NOT_FOUND status
     * @param clazz The model class that wasn't found
     * @param id The id of the object which was mssing
     * @return
     */
    public static <T> Result notFoundError(Class<T> clazz, Object id) {
        ObjectNode result = Json.newObject();
        result.put("status", "error");
        result.put("data", Messages.get("error.not.found", clazz.getSimpleName(), id));
        return Results.notFound(result).as("application/json");
    }

    /**
     * Returns a Result with an error message, and a BAD_REQUEST status
     * @param clazz The model class that wasn't found
     * @param model The model that has the error
     * @return
     */
    public static <T> Result validationError(Class<T> clazz, T model) {
        Form<T> form = new Form<T>(clazz).bind(Json.toJson(model));
        return formError(form);
    }


    /**
     * Returns a Result with an error message, and a BAD_REQUEST status
     * @param form The form with the error
     * @return
     */
    public static Result formError(Form form) {
        ObjectNode result = Json.newObject();
        result.put("status", "error");
        List<String> outErrors = new ArrayList<String>();
        Map<String, List<ValidationError>> errors = form.errors();
        for (String key : errors.keySet()) {
            List<ValidationError> errs = errors.get(key);
            if (errs != null && !errs.isEmpty()) {
                for (ValidationError error : errs) {
                    outErrors.add(key + ": " + play.i18n.Messages.get(error.message(), error.arguments()));
                }
            }
        }
        result.put("data", Json.toJson(outErrors));
        return Results.badRequest(result).as("application/json");
    }

    /**
     * Returns a Result with an error message, and a METHOD_NOT_ALLOWED status
     * @param method The method
     * @param resource The resource
     * @return
     */
    public static Result methodNotAllowed(String method, String resource) {
        ObjectNode result = Json.newObject();
        result.put("status", "error");
        result.put("data", "The '" + method + "' is not allowed for the '" + resource + "' resource.");

        return new play.mvc.Results.Status(JavaResults.MethodNotAllowed(), result, Codec.javaSupported("utf-8"));
    }
}
