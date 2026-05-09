package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for a volunteer RSVP submission. */
public class RsvpRequest {

    @NotBlank
    @JsonProperty("volunteerId")
    private String volunteerId;

    @NotBlank
    @JsonProperty("volunteerName")
    private String volunteerName;

    @NotBlank
    @Email
    @JsonProperty("volunteerEmail")
    private String volunteerEmail;

    public RsvpRequest() {}

    /** @return the volunteer's user ID */
    public String getVolunteerId() { return volunteerId; }
    /** @param volunteerId the volunteer's user ID */
    public void setVolunteerId(String volunteerId) { this.volunteerId = volunteerId; }

    /** @return the volunteer's display name */
    public String getVolunteerName() { return volunteerName; }
    /** @param volunteerName the volunteer's display name */
    public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

    /** @return the volunteer's email address */
    public String getVolunteerEmail() { return volunteerEmail; }
    /** @param volunteerEmail the volunteer's email address */
    public void setVolunteerEmail(String volunteerEmail) { this.volunteerEmail = volunteerEmail; }
}
