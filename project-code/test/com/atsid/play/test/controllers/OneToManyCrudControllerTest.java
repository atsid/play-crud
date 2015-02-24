package com.atsid.play.test.controllers;

import com.avaje.ebean.Query;
import play.db.ebean.Model;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: david.tittsworth
 * Date: 7/2/13
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OneToManyCrudControllerTest<P extends Model /* Not used yet */, C extends Model> extends CrudControllerTest<C> {
    private final Model.Finder<Long, C> childFind;
    private final Long parentId;
    private final String parentFieldName;

    public OneToManyCrudControllerTest(Class<P> parentClass /* Not used yet */, Class<C> newChildClass, String newParentField, Long testParentId, Object reverseClassInstance) {
        super(newChildClass, reverseClassInstance, Arrays.asList(new Class<?>[]{Long.class}), Arrays.asList(new Object[] { testParentId }));
        childFind = new Model.Finder<Long, C>(
            Long.class, newChildClass
        );
        parentFieldName = newParentField;
        parentId = testParentId;
    }

    @Override
    protected Query<C> getParentQuery() {
        return childFind.where().eq(parentFieldName + ".id", parentId).query();
    }
}
