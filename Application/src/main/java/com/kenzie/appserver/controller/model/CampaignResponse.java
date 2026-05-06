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

    @JsonProperty("deadline")
    private String deadline;

    @JsonProperty("category")
    private String category;

    @JsonProperty("user")
    private User user;

    @JsonProperty("supporters")
    private List<Supporter> supporters;

    @JsonProperty("address")
    private String address;

    @JsonProperty("description")
    private String description;

    @JsonProperty("goalAmount")
    private Long goalAmount;

    @JsonProperty("currentAmount")
    private Long currentAmount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("percentFunded")
    private int percentFunded;

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

    public Long getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPercentFunded() { return percentFunded; }
    public void setPercentFunded(int percentFunded) { this.percentFunded = percentFunded; }
}
