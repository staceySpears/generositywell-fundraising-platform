package com.kenzie.appserver.service.model;

/**
 * Semantic action names written to the audit log.
 *
 * <p>Grouped by entity type for readability. The {@code entityType} field on the
 * {@link com.kenzie.appserver.repositories.model.AuditLogRecord} tells the consumer
 * which group applies, but actions are kept in a single enum so callers get compile-time
 * safety without juggling multiple types.
 *
 * <p>Add new values here before writing to the log — never use raw strings for actions.
 */
public enum AuditAction {

    // ── Campaign ───────────────────────────────────────────────────────────────
    /** A new campaign was created. */
    CAMPAIGN_CREATED,
    /** A campaign's mutable fields were updated. */
    CAMPAIGN_UPDATED,
    /** A campaign's fundraising goal was changed. */
    GOAL_UPDATED,
    /** A campaign was moved to CLOSED status. */
    CAMPAIGN_CLOSED,
    /** A campaign was permanently deleted. */
    CAMPAIGN_DELETED,

    // ── Donation ───────────────────────────────────────────────────────────────
    /** A Stripe PaymentIntent was created; payment not yet captured. */
    PAYMENT_INITIATED,
    /** Stripe confirmed the payment succeeded; donation recorded. */
    PAYMENT_SUCCEEDED,
    /** Stripe reported a payment failure. */
    PAYMENT_FAILED,

    // ── Fundraising Event ──────────────────────────────────────────────────────
    /** A new fundraising event was created in PLANNING status. */
    EVENT_CREATED,
    /** An event's mutable fields were updated. */
    EVENT_UPDATED,
    /** An event was moved from PLANNING to SCHEDULED. */
    EVENT_PUBLISHED,
    /** An event was cancelled. */
    EVENT_CANCELLED,
    /** A PLANNING-status event was permanently deleted. */
    EVENT_DELETED,

    // ── RSVP ──────────────────────────────────────────────────────────────────
    /** A volunteer was confirmed for an event. */
    RSVP_CONFIRMED,
    /** A volunteer was added to the waitlist. */
    RSVP_WAITLISTED,
    /** A volunteer cancelled their RSVP. */
    RSVP_CANCELLED,
    /** A waitlisted volunteer was promoted to CONFIRMED after a cancellation. */
    RSVP_PROMOTED_FROM_WAITLIST,

    // ── User ───────────────────────────────────────────────────────────────────
    /** A new user account was registered. */
    USER_REGISTERED,
    /** A user's profile was updated. */
    USER_UPDATED
}
