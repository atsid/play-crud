package com.atsid.play.test.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Junction;
import com.avaje.ebean.Query;
import play.db.ebean.Model;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david.tittsworth
 * Date: 7/2/13
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ManyToManyCrudControllerTest<L extends Model /* Not used yet */, J extends Model, R extends Model> extends CrudControllerTest<R> {
    private final Model.Finder<Long, R> childFind;
    private final Long leftId;
    private Class<J> junctionClass;
    private final String leftFieldName;
    private final String rightFieldName;
    private Class<L> leftClass;

    public ManyToManyCrudControllerTest(Class<L> leftClass /* Not used yet */, Class<J> junctionClass, Class<R> rightClass, String newLeftField, String newRightField, Long testLeftId, Object reverseClassInstance) {
        super(rightClass, reverseClassInstance, Arrays.asList(new Class<?>[]{Long.class}), Arrays.asList(new Object[] { testLeftId }));
        childFind = new Model.Finder<Long, R>(
                Long.class, rightClass
        );
        this.rightFieldName = newRightField;
        this.junctionClass = junctionClass;
        leftFieldName = newLeftField;
        leftId = testLeftId;
    }

    @Override
    protected Query<R> getParentQuery() {
        String idField = "id";
        if (leftFieldName != null) {
            idField = leftFieldName + "." + idField;
        }

        List<J> junctions = Ebean.createQuery(junctionClass).where().eq(idField, leftId).findList();
        Junction j = childFind.orderBy("id").where().disjunction();
        for (J junction : junctions) {
            j.add(Expr.eq("id", ReverseControllerUtils.getFieldValue(ReverseControllerUtils.getFieldValue(junction, rightFieldName), "id")));
        }
        return j.query();
    }
}
