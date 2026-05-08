package com.kenzie.appserver.service.model;

/** Represents an individual who has donated to or supported a fundraising campaign. */
public class Supporter {

    public String id;
    public String name;
    public String email;

    public Supporter(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Supporter() {}

    /** @return the supporter's unique ID */
    public String getId() {
        return id;
    }

    /** @param id the supporter's unique ID */
    public void setId(String id) {
        this.id = id;
    }

    /** @return the supporter's display name */
    public String getName() {
        return name;
    }

    /** @param name the supporter's display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the supporter's email address */
    public String getEmail() {
        return email;
    }

    /** @param email the supporter's email address */
    public void setEmail(String email) {
        this.email = email;
    }
}
