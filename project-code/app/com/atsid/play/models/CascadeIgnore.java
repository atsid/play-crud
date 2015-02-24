package com.atsid.play.models;

import java.lang.annotation.ElementType;

/**
 * Created with IntelliJ IDEA.
 * User: davidtittsworth
 * Date: 9/6/13
 * Time: 9:41 AM
 * To change this template use File | Settings | File Templates.
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.FIELD, ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface CascadeIgnore {
}
