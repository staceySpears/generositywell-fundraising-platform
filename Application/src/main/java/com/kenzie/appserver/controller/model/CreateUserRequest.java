package com.kenzie.appserver.controller.model;//package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.User;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Request body for registering a new user account. */
public class CreateUserRequest {

    @NotBlank
    @JsonProperty("name")
    private String name;

    @NotBlank
    @JsonProperty("email")
    private String email;

    @NotBlank
    @JsonProperty("password")
    private String password;

    public CreateUserRequest(){}

    public CreateUserRequest(String name, String email){
        this.name = name;
        this.email = email;
    }

    /** @return the user's display name */
    public String getName() {
        return name;
    }

    /** @param name the user's display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the user's email address */
    public String getEmail() {
        return email;
    }

    /** @param email the user's email address */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the plain-text password (hashed before storage, never persisted as-is) */
    public String getPassword() {
        return password;
    }

    /** @param password the plain-text password */
    public void setPassword(String password) {
        this.password = password;
    }
}
