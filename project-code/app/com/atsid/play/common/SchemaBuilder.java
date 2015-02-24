package com.atsid.play.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import com.atsid.play.models.schema.FieldDescription;
import com.atsid.play.models.schema.FieldDescriptor;
import com.atsid.play.models.schema.FieldType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import com.atsid.play.controllers.Util;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.annotation.UpdatedTimestamp;
import play.db.ebean.Model;

/**
 * Builds a schema for a given model
 */
public class SchemaBuilder {
    private static HashMap<String, List<FieldDescriptor>> schemas = new
        HashMap<String, List<FieldDescriptor>>();

    /**
     * Builds a schema for the given model
     */
    public static List<FieldDescriptor> buildSchema(Class<? extends Model> modelClass) {
        List<FieldDescriptor> schema;
        String modelName = modelClass.getName();
        if (!schemas.containsKey(modelName)) {
            schema = new ArrayList<FieldDescriptor>();

            for (Field field : Util.getFieldsUpTo(modelClass, Model.class)) {
                FieldDescription fieldDesc = field.getAnnotation(FieldDescription.class);
                String name = field.getName();
                Boolean allowed = fieldDesc != null ? !fieldDesc.ignore() : true;
                if (allowed && java.lang.reflect.Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    Type rType = field.getGenericType();
                    Class clazz = field.getType();
                    if (rType instanceof ParameterizedType) {
                        ParameterizedType pType = (ParameterizedType)rType;
                        clazz = (Class)pType.getActualTypeArguments()[0];
                    }
                    String prettyName = fieldDesc != null && !fieldDesc.displayName().isEmpty() ? fieldDesc.displayName() : prettyFieldName(name);
                    FieldType type = fieldDesc != null && fieldDesc.type() != FieldType.DEFAULT ? fieldDesc.type() : getFilterType(field, clazz, name);
                    String fieldType = clazz.getName();
                    if (!fieldType.startsWith("models.")) {
                        fieldType = "";
                    }
                    schema.add(new FieldDescriptor(prettyName, type, name, null, false, true, fieldType.replaceAll("\\.", "/")));
                }
            }
            schemas.put(modelName, schema);

        } else {
            schema = schemas.get(modelName);
        }
        return schema;
    }

    /**
     * Finds the field descriptor for the given field name
     */
    public static FieldDescriptor lookupFieldDescriptor(Class<? extends Model> clazz, String fieldName) {
        String[] parts = fieldName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean found = false;
            List<FieldDescriptor> mySchema = SchemaBuilder.buildSchema(clazz);
            for (FieldDescriptor descriptor : mySchema) {
                if (descriptor.field.equals(part)) {
                    found = true;
                    // If we are at the last part, and we found the descriptor
                    if (i == parts.length - 1) {
                        return descriptor;
                    } else {
                        // If we have reached a path element that isn't marked as an entity, and we can't
                        // find an appropriate model, then we're done
                        if (descriptor.type != FieldType.ENTITY || descriptor.uri == null || descriptor.uri.isEmpty()) {
                            return null;
                        } else {
                            // Now the new context is the entity model
                            clazz = ModelCache.getModel(descriptor.uri.replaceAll("/", "\\."));
                            break;
                        }
                    }
                }
            }

            if (!found) {
                break;
            }
        }
        return null;
    }

    /**
     * Gets a fitler type from a class
     */
    public static FieldType getFilterType (Field field, Class<?> type, String fieldName) {
        FieldType filterType = FieldType.STRING;
        fieldName = fieldName.toLowerCase();

        if (fieldName.endsWith("time")) {
            filterType = FieldType.STRING; // TODO get TIME type working.
        } else if (Date.class.isAssignableFrom(type) || fieldName.toLowerCase().endsWith("date")) {
            if (field.getAnnotation(CreatedTimestamp.class) != null || field.getAnnotation(UpdatedTimestamp.class) != null) {
                filterType = FieldType.DATETIME;
            } else {
                filterType = FieldType.DATE;
            }
        } else if (java.lang.Number.class.isAssignableFrom(type)) {
            filterType = FieldType.NUMBER;
        } else if (Model.class.isAssignableFrom(type)) {
            filterType = FieldType.ENTITY;
        } else if (Boolean.class.isAssignableFrom(type)) {
            filterType = FieldType.BOOLEAN;
        }

        return filterType;
    }

    /**
     * Makes a display name from a fieldName
     */
    private static String prettyFieldName(String s) {
        String result = s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );

        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}
