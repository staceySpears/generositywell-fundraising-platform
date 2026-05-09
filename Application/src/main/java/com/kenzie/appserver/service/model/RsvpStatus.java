package com.kenzie.appserver.service.model;

/**
 * RSVP state for a volunteer on a fundraising event.
 *
 * <ul>
 *   <li>{@link #CONFIRMED} — volunteer has a guaranteed spot</li>
 *   <li>{@link #WAITLISTED} — volunteer registered after capacity was reached</li>
 *   <li>{@link #CANCELLED} — volunteer cancelled their own RSVP</li>
 * </ul>
 */
public enum RsvpStatus {

    /** Volunteer has a confirmed spot at the event. */
    CONFIRMED,

    /** Volunteer is on the waitlist due to capacity limits. */
    WAITLISTED,

    /** Volunteer cancelled their RSVP. */
    CANCELLED
}
