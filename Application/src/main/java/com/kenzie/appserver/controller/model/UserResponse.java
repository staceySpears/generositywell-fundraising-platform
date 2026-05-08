package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.User;

import java.util.List;

/** API response containing a user's public profile. Password hash is never included. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;


    /** @return the user's unique ID */
    public String getId() {
        return id;
    }

    /** @param id the user's unique ID */
    public void setId(String id) {
        this.id = id;
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
        return email;}

    /** @param email the user's email address */
    public void setEmail(String email) {
        this.email = email;}
}
