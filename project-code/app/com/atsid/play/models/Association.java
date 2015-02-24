package com.atsid.play.models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA.
 * User: davidtittsworth
 * Date: 9/13/13
 * Time: 7:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class Association {
    public Class<? extends Model> clazz;
    public Field field;
    public boolean required;
    public boolean oneToOne;
    public boolean manyToOne;
    public boolean manyToMany;
    public Association(Class<? extends Model> clazz, Field field) {
        this.clazz = clazz;
        this.field = field;
        this.required = field.getAnnotation(Constraints.Required.class) != null || field.getAnnotation(CascadeDelete.class) != null;
        this.oneToOne = field.getAnnotation(OneToOne.class) != null;
        this.manyToOne = field.getAnnotation(ManyToOne.class) != null;
        this.manyToMany = field.getAnnotation(ManyToMany.class) != null;
    }
}
