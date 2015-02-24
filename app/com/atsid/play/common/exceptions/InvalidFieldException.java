package com.atsid.play.common.exceptions;

/**
 * Created by davidtittsworth on 2/23/15.
 */
public class InvalidFieldException extends ServiceError {
    public InvalidFieldException(String field) {
        super("Invalid field: " + field);
    }
}
