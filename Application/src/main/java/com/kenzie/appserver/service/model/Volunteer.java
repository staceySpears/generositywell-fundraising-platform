package com.kenzie.appserver.service.model;

/**
 * Represents a volunteer who has RSVPed to a {@link com.kenzie.appserver.repositories.model.FundraisingEventRecord}.
 *
 * <p>Stored as a delimited string via
 * {@link com.kenzie.appserver.repositories.model.VolunteerTypeConverter}:
 * {@code "id|name|email|rsvpStatus"}.
 */
public class Volunteer {

    private String id;
    private String name;
    private String email;
    private String rsvpStatus; // RsvpStatus.name()

    public Volunteer() {}

    public Volunteer(String id, String name, String email, String rsvpStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.rsvpStatus = rsvpStatus;
    }

    /** @return the volunteer's user ID */
    public String getId() { return id; }
    /** @param id the volunteer's user ID */
    public void setId(String id) { this.id = id; }

    /** @return the volunteer's display name */
    public String getName() { return name; }
    /** @param name the volunteer's display name */
    public void setName(String name) { this.name = name; }

    /** @return the volunteer's email address */
    public String getEmail() { return email; }
    /** @param email the volunteer's email address */
    public void setEmail(String email) { this.email = email; }

    /** @return the RSVP status name (see {@link RsvpStatus}) */
    public String getRsvpStatus() { return rsvpStatus; }
    /** @param rsvpStatus the RSVP status name */
    public void setRsvpStatus(String rsvpStatus) { this.rsvpStatus = rsvpStatus; }
}
