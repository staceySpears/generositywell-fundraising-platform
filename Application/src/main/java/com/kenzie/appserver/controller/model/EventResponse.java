package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.appserver.service.model.User;
import com.kenzie.appserver.service.model.Volunteer;

import java.time.LocalDate;
import java.util.List;

/** Response body for a fundraising event. */
public class EventResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("campaignId")
    private String campaignId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private String location;

    @JsonProperty("eventDate")
    private LocalDate eventDate;

    @JsonProperty("registrationDeadline")
    private LocalDate registrationDeadline;

    @JsonProperty("capacity")
    private Integer capacity;

    @JsonProperty("organizer")
    private User organizer;

    @JsonProperty("volunteers")
    private List<Volunteer> volunteers;

    @JsonProperty("status")
    private String status;

    @JsonProperty("confirmedCount")
    private int confirmedCount;

    @JsonProperty("waitlistedCount")
    private int waitlistedCount;

    @JsonProperty("spotsRemaining")
    private Integer spotsRemaining; // null when capacity is unlimited

    public EventResponse() {}

    /** @return the event ID */
    public String getId() { return id; }
    /** @param id the event ID */
    public void setId(String id) { this.id = id; }

    /** @return the campaign ID */
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

    /** @return the event date */
    public LocalDate getEventDate() { return eventDate; }
    /** @param eventDate the event date */
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    /** @return the RSVP deadline */
    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    /** @param registrationDeadline the RSVP deadline */
    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /** @return the volunteer capacity; {@code null} means unlimited */
    public Integer getCapacity() { return capacity; }
    /** @param capacity the volunteer capacity */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /** @return the event organizer */
    public User getOrganizer() { return organizer; }
    /** @param organizer the event organizer */
    public void setOrganizer(User organizer) { this.organizer = organizer; }

    /** @return all RSVP records */
    public List<Volunteer> getVolunteers() { return volunteers; }
    /** @param volunteers all RSVP records */
    public void setVolunteers(List<Volunteer> volunteers) { this.volunteers = volunteers; }

    /** @return the event status name */
    public String getStatus() { return status; }
    /** @param status the event status name */
    public void setStatus(String status) { this.status = status; }

    /** @return the number of confirmed volunteers */
    public int getConfirmedCount() { return confirmedCount; }
    /** @param confirmedCount the confirmed volunteer count */
    public void setConfirmedCount(int confirmedCount) { this.confirmedCount = confirmedCount; }

    /** @return the number of waitlisted volunteers */
    public int getWaitlistedCount() { return waitlistedCount; }
    /** @param waitlistedCount the waitlisted volunteer count */
    public void setWaitlistedCount(int waitlistedCount) { this.waitlistedCount = waitlistedCount; }

    /** @return remaining confirmed spots; {@code null} when capacity is unlimited */
    public Integer getSpotsRemaining() { return spotsRemaining; }
    /** @param spotsRemaining the remaining confirmed spots */
    public void setSpotsRemaining(Integer spotsRemaining) { this.spotsRemaining = spotsRemaining; }
}
