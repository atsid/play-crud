package com.atsid.play.controllers;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.Play;
import play.db.ebean.Model;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Thinking about changing this from an inheritance pattern to a composition.
 * Controller that provides built-in CRUD operations for flat models.
 * @param <T> The type of model managed by this controller.
 */
public class CrudController<T extends Model> extends BaseCrudController<Long, T> {
    
    public CrudController(Class<T> modelType) {
        super(Long.class, modelType);
    }
    
    protected List<Long> getItemsFromRequest() {
        JsonNode body = request().body().asJson();
        if (body != null && body.isArray()) {
            List<Long> items = new ArrayList<Long>();
            for (final JsonNode itemNode : body) {
                if (itemNode.isNumber()) {
                    items.add(itemNode.asLong());
                } else if (itemNode.isObject()) {
                    items.add(itemNode.get("id").asLong());
                }
            }
            return items;
        }
        return null;
    }
}