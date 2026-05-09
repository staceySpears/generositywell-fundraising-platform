package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Request body for updating the mutable fields of an existing fundraising event. */
public class EventUpdateRequest {

    @NotBlank
    @JsonProperty("id")
    private String id;

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

    public EventUpdateRequest() {}

    /** @return the event ID to update */
    public String getId() { return id; }
    /** @param id the event ID */
    public void setId(String id) { this.id = id; }

    /** @return the updated event name */
    public String getName() { return name; }
    /** @param name the updated event name */
    public void setName(String name) { this.name = name; }

    /** @return the updated event description */
    public String getDescription() { return description; }
    /** @param description the updated description */
    public void setDescription(String description) { this.description = description; }

    /** @return the updated event location */
    public String getLocation() { return location; }
    /** @param location the updated location */
    public void setLocation(String location) { this.location = location; }

    /** @return the updated event date */
    public LocalDate getEventDate() { return eventDate; }
    /** @param eventDate the updated event date */
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    /** @return the updated RSVP deadline */
    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    /** @param registrationDeadline the updated RSVP deadline */
    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /** @return the updated capacity; {@code null} means unlimited */
    public Integer getCapacity() { return capacity; }
    /** @param capacity the updated capacity */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
