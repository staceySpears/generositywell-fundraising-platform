package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request body for recording a direct donation to a campaign. Amount is in cents. */
public class DonationRequest {

    @NotNull
    @Min(1)
    @JsonProperty("amount")
    private Long amount; // in cents

    public DonationRequest() {}

    /** @return the donation amount in cents (must be at least 1) */
    public Long getAmount() { return amount; }
    /** @param amount the donation amount in cents */
    public void setAmount(Long amount) { this.amount = amount; }
}
