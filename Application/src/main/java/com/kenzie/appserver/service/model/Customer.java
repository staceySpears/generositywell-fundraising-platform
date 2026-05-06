package com.kenzie.appserver.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;

public class Customer {

    public String id;
    public String name;
    public String email;

    public Customer(String id, String name, String email){
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Customer(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setEmail(String email){
        this.email = email;
    }
}
