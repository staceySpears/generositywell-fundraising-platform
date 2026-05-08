package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;

import java.util.List;

/** API response representing a campaign's current state. Monetary fields are in cents. */
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

    /** @return the campaign ID */
    public String getId() { return id; }
    /** @param id the campaign ID */
    public void setId(String id) { this.id = id; }

    /** @return the campaign display name */
    public String getName() { return name; }
    /** @param name the campaign display name */
    public void setName(String name) { this.name = name; }

    /** @return the campaign start date (ISO-8601) */
    public String getDate() { return date; }
    /** @param date the start date */
    public void setDate(String date) { this.date = date; }

    /** @return the fundraising deadline (ISO-8601) */
    public String getDeadline() { return deadline; }
    /** @param deadline the fundraising deadline */
    public void setDeadline(String deadline) { this.deadline = deadline; }

    /** @return the campaign category */
    public String getCategory() { return category; }
    /** @param category the campaign category */
    public void setCategory(String category) { this.category = category; }

    /** @return the campaign owner */
    public User getUser() { return user; }
    /** @param user the campaign owner */
    public void setUser(User user) { this.user = user; }

    /** @return the list of supporters */
    public List<Supporter> getSupporters() { return supporters; }
    /** @param supporters the list of supporters */
    public void setSupporters(List<Supporter> supporters) { this.supporters = supporters; }

    /** @return the campaign address */
    public String getAddress() { return address; }
    /** @param address the campaign address */
    public void setAddress(String address) { this.address = address; }

    /** @return the campaign description */
    public String getDescription() { return description; }
    /** @param description the campaign description */
    public void setDescription(String description) { this.description = description; }

    /** @return the fundraising goal in cents */
    public Long getGoalAmount() { return goalAmount; }
    /** @param goalAmount the fundraising goal in cents */
    public void setGoalAmount(Long goalAmount) { this.goalAmount = goalAmount; }

    /** @return the total raised so far, in cents */
    public Long getCurrentAmount() { return currentAmount; }
    /** @param currentAmount the total raised in cents */
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    /** @return the campaign status name (ACTIVE, FUNDED, or CLOSED) */
    public String getStatus() { return status; }
    /** @param status the campaign status name */
    public void setStatus(String status) { this.status = status; }

    /** @return the integer percentage of the goal that has been raised (0–100+) */
    public int getPercentFunded() { return percentFunded; }
    /** @param percentFunded the percent of goal raised */
    public void setPercentFunded(int percentFunded) { this.percentFunded = percentFunded; }
}
