package com.atsid.play.controllers;

import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.Logger;
import play.api.libs.MimeTypes;
import play.libs.F;
import play.libs.Json;
import com.avaje.ebean.Ebean;
import play.mvc.Http;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A utility class for various utility functions
 */
public class Util {
    public Util() {
    }
    public final static String DATE_FORMAT_STR_ISO8601_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public final static F.Tuple[] JSON_CLEAN_REGEXES =
        new F.Tuple[] {
            new F.Tuple("\"passwordHash\":\\s*\"[^\"]+\"", ""),
            new F.Tuple("\"_idGetSet\":\\s*[^,}\"]+", ""),
            new F.Tuple(",,", ","),
            new F.Tuple("\\s*\\{\\s*,", "{"),
            new F.Tuple("\\s*,\\s*\\}", "}")
        };

    public static ObjectMapper objectMapper = new ObjectMapper();
    static {
        DateFormat myFormat = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601_Z, Locale.US);
        myFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Controls the default serialization of dates, to output in this format, rather than epoch time
        //objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(myFormat);
    }

    /**
     * Prints some debugging information for the given request
     * @param request The request
     */
    public static void debugRequest(Http.Request request) {
        Logger.debug("path: " + request.path());
        Logger.debug("meth: " + request.method());
        Logger.debug("body: " + request.body());
        Logger.debug("uri: " + request.uri());
        Logger.debug("version: " + request.version());
        Logger.debug("queryString: " + request.queryString());
        Logger.debug("headers:");
        for (Map.Entry<String, String[]> entry : request.headers().entrySet())
        {
            Logger.debug("\t" + entry.getKey() + "=" + Arrays.toString(entry.getValue()));
        }
    }

    /**
     * Converts the given model object to json
     * @param model The model
     * @return
     */
    public static JsonNode toJson(Object model) {
        return toJson(model, false);
    }

    /**
     * Simple object query
     * @param obj The object
     * @param path The path
     * @return
     */
    public static Object query(Object obj, String path) {
        if (path != null && !path.isEmpty()) {
            int periodIdx = path.indexOf(".");
            if (periodIdx >= 0) {
                String prop = path.substring(0, periodIdx);
                String newPath = path.substring(periodIdx + 1);
                return query(getObjectProperty(obj, prop), newPath);
            }
            return getObjectProperty(obj, path);
        } else {
            return obj;
        }
    }

    /**
     * Gets all fields between the given base class and the parent (exlcuding the parent)
     * @param startClass The start class
     * @param exclusiveParent The anscestor class
     * @return An iterator including the fields
     */
    public static Iterable<Field> getFieldsUpTo(Class<?> startClass, Class<?> exclusiveParent) {

        List<Field> currentClassFields = new ArrayList<Field>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null &&
                (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields =
                    (List<Field>) getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(0, parentClassFields);
        }

        return currentClassFields;
    }

    /**
     * Gets a class type from the given field path
     * @param startClass The class to start the search from
     * @param fieldPath The path to the field
     * @return The class of the field
     * @throws NoSuchFieldException
     */
    public static Class<?> getTypeFromFieldPath(Class startClass, String fieldPath) throws NoSuchFieldException {
        String[] pathArray = fieldPath.split("\\.");
        int i = 0;
        while (i < pathArray.length) {
            startClass = startClass.getField(pathArray[i++]).getType();
        }
        return startClass;
    }

    /**
     * Gets a given property value from the given parent object
     * @param parent The parent object
     * @param prop The property to retrieve
     * @return
     */
    public static Object getObjectProperty(Object parent, String prop) {
        Object child = null;
        if (parent != null) {
            try {
                child = parent.getClass().getField(prop).get(parent);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return child;
    }

    /**
     * Creates a model object from the given json node
     * @param node The JsonNode
     * @param clazz The class to parse into
     * @param <A> The model class
     * @return
     */
    public static <A> A fromJson(JsonNode node, Class<A> clazz) {
        try {
            return objectMapper.treeToValue(node, clazz);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    /**
     * Parses a JsonNode from the given string
     * @param s The json string
     * @return
     */
    public static JsonNode parseJson(String s) {
        try {
            return objectMapper.readValue(s, JsonNode.class);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    /**
     * Gets a date with on the date portion
     * @param date The date
     * @return A date with no time information
     */
    public static Date getDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Gets the time portion of the given date
     * @param date The date
     * @return A date with no date information
     */
    public static Date getTime(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.YEAR, 0);
        c.set(Calendar.DATE, 0);
        return c.getTime();
    }

    /**
     * Converts the given model object to a JsonNode
     * @param model The model
     * @param forceWhole If true, returns the entire object heirarch
     * @return
     */
    public static JsonNode toJson(Object model, boolean forceWhole) {
        // Use the built in Json to parse java objects, Ebean craps out
        if (model != null) {
            if (JsonNode.class.isAssignableFrom(model.getClass())) {
                return (JsonNode)model;
            } else if (forceWhole) {
                // Json in 2.2.* has a setObjectMapper function, which should be set in Global.onStart
                return objectMapper.valueToTree(model);//Json.toJson(model);
            } else if (Collection.class.isAssignableFrom(model.getClass())) {
                ArrayNode arrayNode = Json.newObject().arrayNode();
                JsonContext jc = Ebean.createJsonContext();
                
                for (Object o : (Collection)model) {
                	if (jc.isSupportedType(o.getClass())) {
                		arrayNode.add(Json.parse(sanitizeJsonString(jc.toJsonString(o))));
                	} else {
                		arrayNode.add(Json.parse(sanitizeJsonString(objectToJsonString(o))));
                	}
                }
                return arrayNode;
            }
        }
        
        JsonContext ctx = Ebean.createJsonContext();
        JsonNode json = null;
        if (model != null) {
            //Determine if this model has an ebean descriptor before diving in
            if (ctx.isSupportedType(model.getClass())) {
                json = Json.parse(sanitizeJsonString(Ebean.createJsonContext().toJsonString(model)));
            } else {
                json = Json.parse(sanitizeJsonString(objectToJsonString(model)));
            }
        }
        return json;
    }

    /**
     * Scrubs the given json string using a collection of json regexes
     * @param json The json string to scrub
     * @return
     */
    private static String sanitizeJsonString(String json) {
        if (json != null && !json.isEmpty()) {
            for (F.Tuple<String, String> cleanRegex : JSON_CLEAN_REGEXES) {
                json = json.replaceAll(cleanRegex._1, cleanRegex._2);
            }
            if (json.endsWith(",")) {
                json = json.substring(0, json.length() - 1);
            }

            char start = json.charAt(0);
            char end = json.charAt(json.length() - 1);
            if (start == '[' && end != ']') {
                json += "]";
            } else if (start == '{' && end != '}')  {
                json += "}";
            }
        }
        return json;
    }

    // this call respects @JsonIgnore
    /**
     * Converts the given object to a json string
     * @param o The object
     * @return
     */
    private static String objectToJsonString(Object o) {
        String jsonString = "";
        try {
            jsonString = objectMapper.writeValueAsString(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    /**
     * Extracts a file part from a request, otherwise returns null
     * @return
     */
    public static Http.MultipartFormData.FilePart getFilePartFromRequest(play.mvc.Http.Request request) {
        if (request != null) {
            Http.MultipartFormData formData = request.body().asMultipartFormData();
            if (formData != null) {
                List<Http.MultipartFormData.FilePart> files = formData.getFiles();
                if (files.size() > 0) {
                    return files.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Extracts a mimetype from a file part
     * @param part
     * @return
     */
    public static String getFilePartMimeType(Http.MultipartFormData.FilePart part) {
        // This isn't guaranteed, the user can just send any ol mime type they feel like,
        // or any ol file extension
        String fileName = part.getFilename();
        String mimeType = "";
        if (fileName != null && MimeTypes.forFileName(fileName).nonEmpty()) {
            mimeType = MimeTypes.forFileName(fileName).get();
        } else {
            mimeType = part.getContentType();
        }
        return mimeType;
    }

    /**
     * Determines if the given file part meets the requirements passed in
     * @param part The file part to check
     * @param minSize The minimum size of the file part
     * @param maxSize The maximum size of the file part
     * @param validMimeTypes The list of acceptable mime types
     * @return
     */
    public static boolean isValidFilePart(Http.MultipartFormData.FilePart part, long minSize, long maxSize, HashSet<String> validMimeTypes) {
        if (part == null) {
            return false;
        }

        File f = part.getFile();
        if (minSize >= 0 && f.length() < minSize) {
            return false;
        }

        if (maxSize >= 0 && f.length() > maxSize) {
            return false;
        }

        return validMimeTypes.contains(getFilePartMimeType(part));
    }

    /**
     * Creates a pretty printed string for the given date range
     * @param startDate The start date
     * @param endDate The end date
     * @return
     */
    public static String prettyPrintDateRange(Date startDate, Date endDate) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);

        boolean sameMonth = startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH);
        boolean sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR);
        String result =
                // May 20
                startCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " +
                        startCal.get(Calendar.DAY_OF_MONTH);
        if (!sameYear) {
            result += ", " + startCal.get(Calendar.YEAR);
        }
        result += " - ";
        if (!sameMonth || !sameYear) {
            result += endCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " ";
        }
        result += endCal.get(Calendar.DAY_OF_MONTH) + ", " + endCal.get(Calendar.YEAR);;
        return result;
    }

    /**
     * Returns true if the current database is H2
     * @return
     */
    public static Boolean isUsingH2Database () {
        return play.Play.application().configuration().getString("db.default.url").indexOf("h2") >= 0;
    }
}
