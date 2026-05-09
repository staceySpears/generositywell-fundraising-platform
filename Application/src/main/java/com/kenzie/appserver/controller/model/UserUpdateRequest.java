package com.kenzie.appserver.controller.model;//package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Request body for updating a user's name and email. */
public class UserUpdateRequest {

    @NotBlank
    @JsonProperty("id")
    private String id;

    @NotBlank
    @JsonProperty("name")
    private String name;

    @NotBlank
    @Email
    @JsonProperty("email")
    private String email;

    public UserUpdateRequest(){}

    public UserUpdateRequest(String id, String name, String email){
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /** @return the user ID to update */
    public String getId() {
        return id;
    }

    /** @param id the user ID */
    public void setId(String id) {
        this.id = id;
    }

    /** @return the updated display name */
    public String getName() {
        return name;
    }

    /** @param name the new display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the updated email address */
    public String getEmail() {
        return email;
    }

    /** @param email the new email address */
    public void setEmail(String email) {
        this.email = email;
    }
}
