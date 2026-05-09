package com.kenzie.appserver.controller;

import com.kenzie.appserver.service.CampaignService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Receives and processes Stripe webhook events.
 * Authenticated by Stripe signature rather than JWT — the endpoint is public
 * but rejects any request whose signature does not match the configured secret.
 */
@RestController
@RequestMapping("/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final CampaignService campaignService;
    private final String webhookSecret;

    public StripeWebhookController(
            CampaignService campaignService,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.campaignService = campaignService;
        this.webhookSecret = webhookSecret;
    }

    /**
     * {@code POST /webhooks/stripe} — handles incoming Stripe events.
     * Currently processes {@code payment_intent.succeeded}: looks up the campaign ID
     * from the PaymentIntent metadata and records the donation amount.
     * Always returns 200 to Stripe (after signature validation) so Stripe does not
     * retry on application-level errors — failures are logged instead.
     * The raw request body must be passed through unmodified for signature verification
     * to succeed.
     *
     * @param payload   the raw JSON body bytes
     * @param sigHeader the {@code Stripe-Signature} header value
     * @return 200 "received" on success, 400 on signature failure
     */
    @PostMapping(value = "/stripe", consumes = "application/json")
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(new String(payload), sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
            if (stripeObject.isPresent() && stripeObject.get() instanceof PaymentIntent intent) {
                String campaignId = intent.getMetadata().get("campaignId");
                // donorId is present when the payment was initiated by an authenticated user;
                // null for anonymous payments (e.g. shared payment links with no login)
                String donorId = intent.getMetadata().get("donorId");
                long amount = intent.getAmount();

                if (campaignId != null) {
                    try {
                        campaignService.addDonation(campaignId, amount, donorId);
                        log.info("Donation recorded: campaign={} amount={} donor={}", campaignId, amount, donorId);
                    } catch (Exception e) {
                        // Log but return 200 — Stripe retries on non-2xx, and the payment already succeeded
                        log.error("Failed to record donation for campaign {}: {}", campaignId, e.getMessage());
                    }
                }
            }
        }

        return ResponseEntity.ok("received");
    }
}
