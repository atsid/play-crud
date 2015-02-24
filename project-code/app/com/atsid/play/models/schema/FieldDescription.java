package com.atsid.play.models.schema;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Created by steve on 7/16/14.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface FieldDescription {
    String displayName() default "";
    boolean ignore() default false;
    FieldType type() default FieldType.DEFAULT;
}
