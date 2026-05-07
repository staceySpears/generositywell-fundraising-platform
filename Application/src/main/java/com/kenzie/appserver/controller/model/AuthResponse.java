package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body returned after a successful login containing the signed JWT and user ID. */
public class AuthResponse {

    @JsonProperty("token")
    private final String token;

    @JsonProperty("userId")
    private final String userId;

    public AuthResponse(String token, String userId) {
        this.token = token;
        this.userId = userId;
    }

    /** @return the signed JWT to use as a Bearer token in subsequent requests */
    public String getToken() { return token; }

    /** @return the authenticated user's ID */
    public String getUserId() { return userId; }
}
