package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight response returned by {@code GET /users/{id}/donations}.
 * Contains enough information to render a giving-history entry on the dashboard
 * without exposing the full campaign or other donors' data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationSummaryResponse {

    @JsonProperty("campaignId")
    private String campaignId;

    @JsonProperty("campaignName")
    private String campaignName;

    @JsonProperty("amountInCents")
    private Long amountInCents;

    @JsonProperty("donationDate")
    private String donationDate;

    public DonationSummaryResponse() {}

    /** @return the campaign's unique ID */
    public String getCampaignId() { return campaignId; }
    /** @param campaignId the campaign ID */
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    /** @return the campaign display name */
    public String getCampaignName() { return campaignName; }
    /** @param campaignName the campaign name */
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

    /** @return the donor's cumulative total on this campaign in cents */
    public Long getAmountInCents() { return amountInCents; }
    /** @param amountInCents cumulative donation in cents */
    public void setAmountInCents(Long amountInCents) { this.amountInCents = amountInCents; }

    /** @return ISO date of the most recent donation (yyyy-MM-dd) */
    public String getDonationDate() { return donationDate; }
    /** @param donationDate ISO date string */
    public void setDonationDate(String donationDate) { this.donationDate = donationDate; }
}
