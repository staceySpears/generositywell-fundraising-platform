package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Request body for creating a new fundraising event. */
public class CreateEventRequest {

    @NotBlank
    @JsonProperty("campaignId")
    private String campaignId;

    @NotBlank
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private String location;

    @NotNull
    @JsonProperty("eventDate")
    private LocalDate eventDate;

    @NotNull
    @JsonProperty("registrationDeadline")
    private LocalDate registrationDeadline;

    @Min(1)
    @JsonProperty("capacity")
    private Integer capacity;

    @NotNull
    @JsonProperty("organizer")
    private User organizer;

    public CreateEventRequest() {}

    /** @return the ID of the campaign this event belongs to */
    public String getCampaignId() { return campaignId; }
    /** @param campaignId the campaign ID */
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    /** @return the event display name */
    public String getName() { return name; }
    /** @param name the event display name */
    public void setName(String name) { this.name = name; }

    /** @return the event description */
    public String getDescription() { return description; }
    /** @param description the event description */
    public void setDescription(String description) { this.description = description; }

    /** @return the event location */
    public String getLocation() { return location; }
    /** @param location the event location */
    public void setLocation(String location) { this.location = location; }

    /** @return the date of the event */
    public LocalDate getEventDate() { return eventDate; }
    /** @param eventDate the event date */
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    /** @return the RSVP deadline */
    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    /** @param registrationDeadline the RSVP deadline */
    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /** @return the maximum number of confirmed volunteers; {@code null} means unlimited */
    public Integer getCapacity() { return capacity; }
    /** @param capacity the volunteer capacity */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /** @return the event organizer */
    public User getOrganizer() { return organizer; }
    /** @param organizer the event organizer */
    public void setOrganizer(User organizer) { this.organizer = organizer; }
}
