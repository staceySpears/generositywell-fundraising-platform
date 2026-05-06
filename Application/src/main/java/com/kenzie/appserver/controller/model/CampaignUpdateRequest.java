package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CampaignUpdateRequest {

    @NotBlank
    @JsonProperty("id")
    private String id;

    @NotBlank
    @JsonProperty("name")
    private String name;

    @NotBlank
    @JsonProperty("date")
    private String date;

    @NotBlank
    @JsonProperty("deadline")
    private String deadline;

    @NotBlank
    @JsonProperty("category")
    private String category;

    @NotNull
    @JsonProperty("user")
    private User user;

    @NotNull
    @JsonProperty("supporters")
    private List<Supporter> supporters;

    @JsonProperty("address")
    private String address;

    @NotBlank
    @JsonProperty("description")
    private String description;

    @NotNull
    @Min(1)
    @JsonProperty("goalAmount")
    private Long goalAmount; // in cents

    public CampaignUpdateRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Supporter> getSupporters() { return supporters; }
    public void setSupporters(List<Supporter> supporters) { this.supporters = supporters; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getGoalAmount() { return goalAmount; }
    public void setGoalAmount(Long goalAmount) { this.goalAmount = goalAmount; }
}
