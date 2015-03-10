package com.atsid.play.models;

import com.atsid.play.common.ModelCache;
import org.reflections.Reflections;
import play.db.ebean.Model;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david.tittsworth
 * Date: 8/7/13
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssociationFinder {
    private static HashMap<String, List<Association>> associations = new HashMap<String, List<Association>> ();

    /**
     * Returns true if the given field has the given annotation
     * @param f
     * @param c
     * @return True if this given Field has the given annotation class
     */
    private static boolean hasAnnotation(Field f, Class c) {
        return f.getAnnotation(c) != null;
    }

    /**
     * Finds a list of associations between the given model class, and all other models
     * @param clazz The class to find the associations for
     * @param <M> The model type
     * @return The list of associated model classes to the given class
     */
    public static <M extends Model> List<Class<? extends Model>> findClassAssociations(Class<M> clazz) {
        return findClassAssociations(clazz, false);
    }

    /**
     * Finds a list of associations between the given model class, and all other models
     * @param clazz The class to find the associations for
     * @param <M> The model type
     * @param requiredOnly Returns only assocations that are marked with the Constraints.Required annotation.
     * @return The list of associated model classes to the given class
     */
    public static <M extends Model> List<Class<? extends Model>> findClassAssociations(Class<M> clazz, boolean requiredOnly) {
        return filterClassAssociations(findAssociations(clazz), requiredOnly);
    }

    /**
     * Finds all assocations between one model and another
     * @param clazz
     * @return
     */
    public static List<Association> findAssociations(Class<? extends Model> clazz) {
        if (!associations.containsKey(clazz.getName())) {
            List<Association> list = new ArrayList<Association>();
            for (Class c : ModelCache.getModels()) {
                for (Field f : c.getFields()) {
                    if (f.getType().equals(clazz) && hasAssociations(f)) {
                        list.add(new Association(c, f));
                    }
                }
            }
            associations.put(clazz.getName(), list);
        }
        return associations.get(clazz.getName());
    }

    public static Association findAssocation(Class<? extends Model> from, Class<? extends Model> to) {
        List<Association> list = findAssociations(to);
        for (Association assoc : list) {
            if (assoc.clazz == from) {
                return assoc;
            }
        }
        return null;
    }

    /**
     * Filters the associations for hard/all associations
     * @param associations
     * @param hardOnly
     * @return
     */
    private static List<Class<? extends Model>> filterClassAssociations(List<Association> associations, boolean hardOnly) {
        List<Class<? extends Model>> filtered = new ArrayList<Class<? extends Model>>();
        for (Association assoc : associations) {
            if (!hardOnly || assoc.required) {
                filtered.add(assoc.clazz);
            }
        }
        return filtered;
    }

    /**
     * Returns whether or not the given field has any associations with another
     * @param f The field to check
     * @return
     */
    private static boolean hasAssociations(Field f) {
        return
            hasAnnotation(f, ManyToOne.class) ||
            hasAnnotation(f, ManyToMany.class) ||
            hasAnnotation(f, OneToOne.class);
    }
}