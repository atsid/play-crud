package com.atsid.play.common.exceptions;

/**
 * Created by davidtittsworth on 2/23/15.
 */
public class ServiceError extends RuntimeException {
    public ServiceError() {
        this("Unknown Error");
    }
    public ServiceError(String message) {
        super(message);
    }
}
