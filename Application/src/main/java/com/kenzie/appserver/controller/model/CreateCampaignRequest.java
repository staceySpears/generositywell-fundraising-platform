package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateCampaignRequest {

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
    @JsonProperty("supporters")
    private List<Supporter> supporters;

    @NotBlank
    @JsonProperty("address")
    private String address;

    @NotBlank
    @JsonProperty("description")
    private String description;

    public CreateCampaignRequest() {}

    public CreateCampaignRequest(String name, String date, User user, List<Supporter> supporters,
                                 String address, String description) {
        this.name = name;
        this.date = date;
        this.user = user;
        this.supporters = supporters;
        this.address = address;
        this.description = description;
    }

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
