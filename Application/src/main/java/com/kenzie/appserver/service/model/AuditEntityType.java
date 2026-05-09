package com.kenzie.appserver.service.model;

/**
 * Identifies which domain entity an {@link com.kenzie.appserver.repositories.model.AuditLogRecord}
 * describes.
 *
 * <p>Used as the {@code entityType} field in the audit log so that consumers (dashboard queries,
 * Salesforce sync, EventBridge routing rules) can filter by entity category without inspecting
 * the payload.
 */
public enum AuditEntityType {

    /** A fundraising campaign. */
    CAMPAIGN,

    /** A financial donation tied to a campaign. */
    DONATION,

    /** A volunteer fundraising event. */
    FUNDRAISING_EVENT,

    /** A volunteer RSVP on a fundraising event. */
    RSVP,

    /** A platform user account. */
    USER
}
