package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("date")
    private String date;

    @JsonProperty("user")
    private User user;

    @JsonProperty("supporters")
    private List<Supporter> supporters;

    @JsonProperty("address")
    private String address;

    @JsonProperty("description")
    private String description;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Supporter> getSupporters() { return supporters; }
    public void setSupporters(List<Supporter> supporters) { this.supporters = supporters; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
