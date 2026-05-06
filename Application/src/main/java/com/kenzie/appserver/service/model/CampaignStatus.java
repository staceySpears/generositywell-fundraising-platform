package com.kenzie.appserver.service.model;

public enum CampaignStatus {
    ACTIVE,   // accepting donations, goal not yet met
    FUNDED,   // goal met, still accepting (overfunding allowed)
    CLOSED    // no longer accepting donations
}
