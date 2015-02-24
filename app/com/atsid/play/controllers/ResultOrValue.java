package com.atsid.play.controllers;

import play.mvc.Result;

/**
 * Object that wraps either a result or a value
 * @param <G> The type of the value
 */
public class ResultOrValue<G> {
    public G value;
    public Result result;
    public ResultOrValue(G value) { this.value = value; }
    public ResultOrValue(Result result) { this.result = result; }
}