package com.kenzie.appserver.service.model;

/**
 * Lifecycle states for a {@link com.kenzie.appserver.repositories.model.FundraisingEventRecord}.
 *
 * <p>Valid transitions:
 * <pre>
 *   PLANNING → SCHEDULED → IN_PROGRESS → COMPLETED
 *                       ↘                ↗
 *                        CANCELLED ←────
 * </pre>
 * The organiser moves the event from PLANNING to SCHEDULED once the details are confirmed;
 * a scheduled job or manual action moves it to IN_PROGRESS / COMPLETED based on dates.
 * CANCELLED may be set from SCHEDULED or IN_PROGRESS.
 */
public enum EventStatus {

    /** Event is being planned; volunteers cannot yet RSVP. */
    PLANNING,

    /** Event is confirmed; volunteers may submit RSVPs up to the registration deadline. */
    SCHEDULED,

    /** Event is currently in progress; new RSVPs are no longer accepted. */
    IN_PROGRESS,

    /** Event has concluded successfully. */
    COMPLETED,

    /** Event was cancelled before or during execution. */
    CANCELLED
}
