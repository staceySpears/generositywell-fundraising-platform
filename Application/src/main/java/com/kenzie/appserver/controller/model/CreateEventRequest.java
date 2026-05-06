package com.kenzie.appserver.controller.model;//package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Attendee;
import com.kenzie.appserver.service.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateEventRequest {


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
    private List<Attendee> listOfAttending;

    @NotBlank
    @JsonProperty("address")
    private String address;

    @NotBlank
    @JsonProperty("description")
    private String description;


    public CreateEventRequest(){}

    public CreateEventRequest(String name, String date, User user, List<Attendee> ListOfAttending,
                              String address, String description){
        this.name = name;
        this.date = date;
        this.user = user;
        this.listOfAttending = ListOfAttending;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Attendee> getListOfAttending() {
        return listOfAttending;
    }

    public void setListOfAttending(List<Attendee> listOfAttending) {
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
}
