package com.atsid.play.controllers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by davidtittsworth on 9/16/14.
 */
public class ServiceParams {
    public int offset;
    public Integer count;
    public Set<String> fetches;
    public Set<String> fields;
    public String[] orderBy;
    public String queryString;

    public ServiceParams() {
        this.fetches = setify(null, "");
        this.fields = setify(null, "");
    }

    /**
     * Constructs a ServiceParams
     * @param offset An offset from the first item to start filtering from.  Used for paging.
     * @param count The total count to query.  This is the length of items to query after the offset.  Used for paging.
     * @param orderBy Order the queried models in order by the given properties of the model.
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param queryString Filter the models with a comma-delimited query string in the format of "property:value".
     */
    public ServiceParams(final int offset, final Integer count, final String orderBy, final String fields, final String fetches, final String queryString) {
        this(offset, count, orderBy, fields, fetches, queryString, "");
    }

    /**
     * Constructs a ServiceParams
     * @param offset An offset from the first item to start filtering from.  Used for paging.
     * @param count The total count to query.  This is the length of items to query after the offset.  Used for paging.
     * @param orderBy Order the queried models in order by the given properties of the model.
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param queryString Filter the models with a comma-delimited query string in the format of "property:value".
     * @param basePath A string containing the base path to all of the fields in the service params
     */
    public ServiceParams(final int offset, final Integer count, final String orderBy, final String fields, final String fetches, final String queryString, final String basePath) {
        this.offset = offset;
        this.count = count;
        this.orderBy = addFieldPathToCommaDelmitedString(orderBy, basePath);
        this.fetches = setify(fetches, basePath);
        this.fields = setify(fields, basePath);
        if (queryString != null && !queryString.isEmpty()) {
            this.queryString = basePath + queryString.replace("AND", "AND" + basePath).replace("OR", "OR" + basePath);
        } else {
            this.queryString = null;
        }
    }

    /**
     * Constructs a ServiceParams
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     */
    public ServiceParams(final String fields, final String fetches) {
        this(fields, fetches, "");
    }

    /**
     * Constructs a ServiceParams
     * @param fields The fields, or properties, of the models to retrieve.
     * @param fetches If the model has 1to1 relationships, use this to retrieve those relationships.  By default, returns the id of each relationship.
     * @param basePath A string containing the base path to all of the fields in the service params
     */
    public ServiceParams(final String fields, final String fetches, final String basePath) {
        this.fetches = setify(fetches, basePath);
        this.fields = setify(fields, basePath);
    }

    /**
     * Converts a comma delimited list into a set
     * @param commaString The comma delimited string
     * @param basePath A string containing the base path to all of the fields in the service params
     * @return
     */
    protected Set<String> setify(String commaString, String basePath) {
        LinkedHashSet<String> hash = new LinkedHashSet<String>();
        if (commaString != null) {
            String[] parts = addFieldPathToCommaDelmitedString(commaString.replaceAll(" ", ""), basePath);
            for (String part : parts) {
                hash.add(part);
            }
        }
        return hash;
    }

    /**
     * Adds the base path to each of the values in the given comma delimited string
     * @param cds The comma delimited string
     * @param basePath A string containing the base path to all of the fields in the service params
     * @return
     */
    private String[] addFieldPathToCommaDelmitedString(String cds, String basePath) {
        if (cds != null && !cds.trim().isEmpty()) {
            String[] parts = cds.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = getFinalFieldPath(parts[i], basePath);
            }
            return parts;
        }
        return null;
    }

    /**
     * Combines the given field with the base path
     * @param field The field
     * @param basePath The base path
     * @return
     */
    private String getFinalFieldPath(String field, String basePath) {
        return basePath + field;
    }
}
