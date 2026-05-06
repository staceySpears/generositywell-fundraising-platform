package com.kenzie.appserver.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public String getClientSecret() { return clientSecret; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public Long getAmount() { return amount; }
}
