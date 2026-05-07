package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request body for creating a Stripe PaymentIntent. Amount is in cents. */
public class PaymentIntentRequest {

    @NotNull
    @Min(1)
    @JsonProperty("amount")
    private Long amount; // in cents

    public PaymentIntentRequest() {}

    /** @return the amount to charge in cents (must be at least 1) */
    public Long getAmount() { return amount; }
    /** @param amount the charge amount in cents */
    public void setAmount(Long amount) { this.amount = amount; }
}
