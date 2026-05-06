package com.kenzie.appserver.controller.model;//package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Customer;
import com.kenzie.appserver.service.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class EventUpdateRequest {

    @NotBlank
    @JsonProperty("id")
    private String id;

    @NotBlank
    @JsonProperty("name")
    private String name;

    @NotBlank
    @JsonProperty("date")
    private String date;

    @NotNull
    @JsonProperty("user")
    private User user;

    @NotNull
    @JsonProperty("listOfAttending")
    private List<Customer> listOfAttending;

    @NotBlank
    @JsonProperty("address")
    private String address;

    @NotBlank
    @JsonProperty("description")
    private String description;

    public EventUpdateRequest(){}

    public EventUpdateRequest(String id, String name, String date, User user, List<Customer> getListOfAttending,
                              String address, String description){
        this.id = id;
        this.name = name;
        this.date = date;
        this.user = user;
        this.listOfAttending = getListOfAttending;
        this.address = address;
        this.description = description;
    }

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<Customer> getListOfAttending() {
        return listOfAttending;
    }

    public void setListOfAttending(List<Customer> listOfAttending) {
        this.listOfAttending = listOfAttending;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
