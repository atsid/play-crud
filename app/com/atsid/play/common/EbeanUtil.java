package com.atsid.play.common;

import com.avaje.ebean.bean.EntityBean;

import java.util.Arrays;
import java.util.List;

/**
 * User: alikalarsen
 * Date: 11/4/13
 */
public class EbeanUtil {
    public static Object getFieldValue(EntityBean model, String field) {
        List<String> fields = Arrays.asList(model._ebean_getFieldNames());
        int fieldIndex = fields.indexOf(field);
        return model._ebean_getFieldIntercept(fieldIndex, model);
    }

    public static void setFieldValue(EntityBean target, String fieldName, Object fieldValue) {
        List<String> fields = Arrays.asList(target._ebean_getFieldNames());
        int fieldIndex = fields.indexOf(fieldName);
        target._ebean_setField(fieldIndex, target, fieldValue);
    }
}
