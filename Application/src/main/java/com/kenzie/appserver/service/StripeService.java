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
     * Both {@code campaignId} and {@code donorId} (when present) are stored in the
     * PaymentIntent metadata so the webhook handler can record the donation—and
     * attribute it to the authenticated donor—after payment succeeds.
     * Throws 502 if the Stripe API returns an error.
     *
     * @param campaignId    the local campaign ID to attach to the payment intent metadata
     * @param amountInCents the amount to charge in the smallest currency unit (cents)
     * @param donorId       the authenticated user ID, or {@code null} for anonymous payments
     * @return the client secret, payment intent ID, and amount
     */
    public PaymentIntentResponse createPaymentIntent(String campaignId, Long amountInCents, String donorId) {
        try {
            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putMetadata("campaignId", campaignId)
                    .addPaymentMethodType("card");

            if (donorId != null) {
                builder.putMetadata("donorId", donorId);
            }

            PaymentIntent intent = PaymentIntent.create(builder.build());
            return new PaymentIntentResponse(intent.getClientSecret(), intent.getId(), amountInCents);

        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage());
        }
    }
}
