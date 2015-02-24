package com.atsid.play.test.controllers;

import play.test.FakeRequest;

/**
 * User: alikalarsen
 * Date: 4/11/13
 */
public class TestHelper {

    private static final String PLAYAUTH_PROVIDER_KEY = "pa.p.id";
    private static final String PLAYAUTH_USER_KEY = "pa.u.id";
    private static final String PLAYAUTH_EXPIRES_KEY_= "pa.u.exp";

    public static FakeRequest getFakeLoggedinRequest() {
        FakeRequest request = new FakeRequest();
        Long expiration = System.currentTimeMillis() + (1000 * 60 * 30); //expires in 30 mins
        request.withSession("pa.u.exp", expiration.toString());
        request.withSession("pa.p.id" , "password");
        request.withSession("pa.u.id" , "sally.joe@gmail.com");

        return request;
    }
}
