package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body returned after a Stripe PaymentIntent is created. */
public class PaymentIntentResponse {

    @JsonProperty("clientSecret")
    private final String clientSecret;

    @JsonProperty("paymentIntentId")
    private final String paymentIntentId;

    @JsonProperty("amount")
    private final Long amount;

    public PaymentIntentResponse(String clientSecret, String paymentIntentId, Long amount) {
        this.clientSecret = clientSecret;
        this.paymentIntentId = paymentIntentId;
        this.amount = amount;
    }

    /** @return the client secret for front-end Stripe.js confirmation */
    public String getClientSecret() { return clientSecret; }

    /** @return the Stripe PaymentIntent ID */
    public String getPaymentIntentId() { return paymentIntentId; }

    /** @return the charge amount in cents */
    public Long getAmount() { return amount; }
}
