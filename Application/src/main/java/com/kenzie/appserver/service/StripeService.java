package com.kenzie.appserver.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.kenzie.appserver.controller.model.PaymentIntentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Wraps the Stripe API for payment intent creation. */
@Service
public class StripeService {

    /**
     * Creates a Stripe PaymentIntent for the given campaign and amount.
     * The campaign ID is stored in the PaymentIntent metadata so the webhook
     * handler can record the donation after payment succeeds.
     * Throws 502 if the Stripe API returns an error.
     *
     * @param campaignId   the local campaign ID to attach to the payment intent metadata
     * @param amountInCents the amount to charge in the smallest currency unit (cents)
     * @return the client secret, payment intent ID, and amount
     */
    public PaymentIntentResponse createPaymentIntent(String campaignId, Long amountInCents) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putMetadata("campaignId", campaignId)
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return new PaymentIntentResponse(intent.getClientSecret(), intent.getId(), amountInCents);

        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage());
        }
    }
}
