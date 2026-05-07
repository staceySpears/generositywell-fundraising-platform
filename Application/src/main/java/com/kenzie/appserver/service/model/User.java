package com.kenzie.appserver.service.model;

/** Represents the owner of a campaign or a participant in the platform. */
public class User {
    private String id;
    private String name;
    private String email;

    public User(){}

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

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
        return email;
    }

    /** @param email the user's email address */
    public void setEmail(String email) {
        this.email = email;
    }
}
