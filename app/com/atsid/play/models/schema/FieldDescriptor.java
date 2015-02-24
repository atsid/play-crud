package com.atsid.play.models.schema;

/**
 * Created by davidtittsworth on 7/14/14.
 */
public class FieldDescriptor {
    public FieldDescriptor() {}

    public FieldDescriptor(String name, FieldType type, String field) {
        this(name, type, field, null);
    }

    public FieldDescriptor(String name, FieldType type, String field, String parentField) {
        this(name, type, field, parentField, false, false, null);
    }

    public FieldDescriptor(String name, FieldType type, String field, String parentField, Boolean required, Boolean isColumn, String uri) {
        this.name = name;
        this.type = type;
        this.field = field;
        this.parentField = parentField;
        this.isColumn = isColumn;
        this.required = required;
        this.uri = uri;
    }

    /**
     * A generic name for the descriptor
     */
    public String name;

    /**
     * The field type
     */
    public FieldType type;

    /**
     * The field name
     */
    public String field;

    /**
     * The class type uri
     */
    public String uri;

    /**
     * The parent field name
     */
    public String parentField;

    /**
     * A boolean indicating whether or not this is an actual field
     */
    public Boolean isColumn = false;

    /**
     * A boolean indicating whether or not this field is required
     */
    public Boolean required = false;
}
