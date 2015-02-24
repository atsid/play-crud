package com.atsid.play.common;

import com.atsid.play.models.AbstractBaseModel;
import org.reflections.Reflections;
import play.db.ebean.Model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by davidtittsworth on 8/13/14.
 */
public class ModelCache {
    private static Reflections reflections = new Reflections("models");
    private static HashMap<String, Class<? extends Model>> models = new HashMap<String, Class<? extends Model>>();

    static {
        if (models.isEmpty()) {
            populateModels();
        }
    }

    /**
     * Gets a model from a name
     * @param name The name of the model
     * @return
     */
    public static Class<? extends Model> getModel(String name) {
        if (models.containsKey(name)) {
            return models.get(name);
        }
        return null;
    }

    /**
     * Gets all of the models in the project
     * @return
     */
    public static Collection<Class<? extends Model>> getModels() {
        return models.values();
    }

    /**
     * Populates the list of models
     */
    private static void populateModels() {
        Set<Class<? extends AbstractBaseModel>> subTypes = reflections.getSubTypesOf(AbstractBaseModel.class);
        models.clear();
        for (Class<? extends AbstractBaseModel> subType : subTypes) {
            models.put(subType.getName(), subType);
        }
    }
}
