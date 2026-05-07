package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** Request body for authenticating a user with email and password. */
public class LoginRequest {

    @NotBlank
    @JsonProperty("email")
    private String email;

    @NotBlank
    @JsonProperty("password")
    private String password;

    public LoginRequest() {}

    /** @return the user's email address */
    public String getEmail() { return email; }
    /** @param email the user's email address */
    public void setEmail(String email) { this.email = email; }

    /** @return the plain-text password to verify against the stored hash */
    public String getPassword() { return password; }
    /** @param password the plain-text password */
    public void setPassword(String password) { this.password = password; }
}
