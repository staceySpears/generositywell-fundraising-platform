package com.kenzie.appserver.service.model;

/**
 * Lifecycle states for a fundraising campaign.
 * Transitions: ACTIVE → FUNDED (when goal is reached) → CLOSED (when creator closes it).
 * CLOSED campaigns no longer accept donations.
 */
public enum CampaignStatus {
    ACTIVE,   // accepting donations, goal not yet met
    FUNDED,   // goal met, still accepting (overfunding allowed)
    CLOSED    // no longer accepting donations
}
