package com.kenzie.appserver.service.model;

public class Attendee {

    public String id;
    public String name;
    public String email;

    public Attendee(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Attendee() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
