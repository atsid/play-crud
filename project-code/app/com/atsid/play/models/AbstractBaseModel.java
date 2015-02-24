package com.atsid.play.models;

import com.avaje.ebean.Ebean;
import play.db.ebean.Model;

import javax.persistence.MappedSuperclass;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: davidtittsworth
 * Date: 9/6/13
 * Time: 8:01 AM
 * To change this template use File | Settings | File Templates.
 */
@MappedSuperclass
public abstract class AbstractBaseModel extends Model {

    @Override
    public void delete() {
        onPreDelete();
        super.delete();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void delete(String s) {
        onPreDelete();
        super.delete(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
     * Function to be called before delete
     */
    private void onPreDelete() {
        List<Association> associations =
                AssociationFinder.findAssociations(this.getClass());
        for (Association assoc : associations) {

            // Ignore if the class has the ignore on the field
            if (assoc.clazz.getAnnotation(CascadeIgnore.class) == null &&
                assoc.field.getAnnotation(CascadeIgnore.class) == null) {

                List<? extends Model> childModels =
                        Ebean.createQuery(assoc.clazz).where().eq(assoc.field.getName() + ".id", getId()).findList();
                for (Model child : childModels) {
                    // If it is required, delete it, otherwise set it to null
                    if (assoc.required) {
                        child.delete();
                    } else {
                        try {
                            assoc.field.set(child, null);
                            Set<String> set = new HashSet<String>();
                            set.add(assoc.field.getName());
                            Ebean.update(child, set);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the id of the current model
     * @return
     */
    private Object getId() {
        try {
            return this.getClass().getField("id").get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchFieldException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }
}
