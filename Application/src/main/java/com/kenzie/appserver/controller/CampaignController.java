package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.controller.model.DonationRequest;
import com.kenzie.appserver.controller.model.PaymentIntentRequest;
import com.kenzie.appserver.controller.model.PaymentIntentResponse;
import com.kenzie.appserver.service.CampaignService;
import com.kenzie.appserver.service.StripeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** REST controller for campaign CRUD, donations, and payment intent creation. */
@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final StripeService stripeService;

    CampaignController(CampaignService campaignService, StripeService stripeService) {
        this.campaignService = campaignService;
        this.stripeService = stripeService;
    }

    /**
     * {@code GET /campaigns/{id}} — returns a single campaign by ID.
     * Public endpoint; no authentication required.
     *
     * @param id the campaign ID
     * @return 200 with the campaign, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable("id") String id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    /**
     * {@code POST /campaigns} — creates a new campaign.
     * Requires a valid JWT. Returns 201 with a Location header pointing to the new resource.
     *
     * @param request the campaign creation request
     * @return 201 with the created campaign
     */
    @PostMapping
    public ResponseEntity<CampaignResponse> addCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = campaignService.addNewCampaign(request);
        return ResponseEntity.created(URI.create("/campaigns/" + response.getId())).body(response);
    }

    /**
     * {@code PUT /campaigns/{campaignId}} — updates a campaign's mutable fields.
     * The request body must include the caller's user ID; the service verifies it matches
     * the stored campaign creator before applying changes. JWT is not enforced at this
     * endpoint boundary — ownership is checked via the user ID in the request body.
     *
     * @param request the update payload (must include the caller's user ID)
     * @return 200 with the updated campaign
     * @throws org.springframework.web.server.ResponseStatusException 404 if not found,
     *         403 if the caller is not the owner, 409 if the campaign is closed
     */
    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(@Valid @RequestBody CampaignUpdateRequest request) {
        CampaignResponse response = campaignService.updateCampaign(request);
        return ResponseEntity.ok(response);
    }

    /**
     * {@code POST /campaigns/{campaignId}/payment-intent} — creates a Stripe PaymentIntent.
     * The campaign ID is embedded in the PaymentIntent metadata so the webhook can record
     * the donation after payment succeeds. Requires a valid JWT.
     *
     * @param campaignId the target campaign
     * @param request    the payment amount
     * @return 200 with the client secret for front-end confirmation
     */
    @PostMapping("/{campaignId}/payment-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @PathVariable("campaignId") String campaignId,
            @Valid @RequestBody PaymentIntentRequest request) {
        PaymentIntentResponse response = stripeService.createPaymentIntent(campaignId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    /**
     * {@code POST /campaigns/{campaignId}/donate} — records a donation directly.
     * This is the direct path used for testing and non-card flows; the Stripe webhook
     * is the production path for card payments. Public endpoint.
     *
     * @param campaignId the campaign to donate to
     * @param request    the donation amount in cents
     * @return 200 with the updated campaign
     */
    @PostMapping("/{campaignId}/donate")
    public ResponseEntity<CampaignResponse> donate(
            @PathVariable("campaignId") String campaignId,
            @Valid @RequestBody DonationRequest request) {
        CampaignResponse response = campaignService.addDonation(campaignId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    /**
     * {@code POST /campaigns/{campaignId}/close} — closes a campaign.
     * Only the campaign owner (matched by JWT subject) may close it. Requires a valid JWT.
     *
     * @param campaignId     the campaign to close
     * @param authentication the authenticated principal; {@code getName()} returns the user ID
     * @return 200 with the updated campaign in CLOSED status
     */
    @PostMapping("/{campaignId}/close")
    public ResponseEntity<CampaignResponse> closeCampaign(
            @PathVariable("campaignId") String campaignId,
            Authentication authentication) {
        CampaignResponse response = campaignService.closeCampaign(campaignId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * {@code DELETE /campaigns/{campaignId}} — permanently deletes a campaign.
     * Requires a valid JWT.
     *
     * @param campaignId the campaign to delete
     * @return 204 on success, 400 if ID is blank, 404 if not found
     */
    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaignById(@PathVariable("campaignId") String campaignId) {
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /campaigns/all} — returns all campaigns.
     * Public endpoint. Performs a full table scan — avoid polling at high frequency.
     *
     * @return 200 with the list of all campaigns
     */
    @GetMapping("/all")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }
}
