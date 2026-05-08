package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request body for updating an existing campaign's mutable fields. Monetary amounts are in cents. */
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

    /** @return the campaign ID to update */
    public String getId() { return id; }
    /** @param id the campaign ID */
    public void setId(String id) { this.id = id; }

    /** @return the updated campaign name */
    public String getName() { return name; }
    /** @param name the campaign name */
    public void setName(String name) { this.name = name; }

    /** @return the updated start date (ISO-8601) */
    public String getDate() { return date; }
    /** @param date the start date */
    public void setDate(String date) { this.date = date; }

    /** @return the updated deadline (ISO-8601) */
    public String getDeadline() { return deadline; }
    /** @param deadline the fundraising deadline */
    public void setDeadline(String deadline) { this.deadline = deadline; }

    /** @return the updated category */
    public String getCategory() { return category; }
    /** @param category the campaign category */
    public void setCategory(String category) { this.category = category; }

    /** @return the campaign owner */
    public User getUser() { return user; }
    /** @param user the campaign owner */
    public void setUser(User user) { this.user = user; }

    /** @return the updated list of supporters */
    public List<Supporter> getSupporters() { return supporters; }
    /** @param supporters the list of supporters */
    public void setSupporters(List<Supporter> supporters) { this.supporters = supporters; }

    /** @return the updated address */
    public String getAddress() { return address; }
    /** @param address the campaign address */
    public void setAddress(String address) { this.address = address; }

    /** @return the updated description */
    public String getDescription() { return description; }
    /** @param description the campaign description */
    public void setDescription(String description) { this.description = description; }

    /** @return the updated fundraising goal in cents */
    public Long getGoalAmount() { return goalAmount; }
    /** @param goalAmount the fundraising goal in cents */
    public void setGoalAmount(Long goalAmount) { this.goalAmount = goalAmount; }
}
