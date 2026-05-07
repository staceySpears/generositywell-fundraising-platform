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

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final StripeService stripeService;

    CampaignController(CampaignService campaignService, StripeService stripeService) {
        this.campaignService = campaignService;
        this.stripeService = stripeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable("id") String id) {
        CampaignResponse response = campaignService.getCampaignById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> addCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = campaignService.addNewCampaign(request);
        return ResponseEntity.created(URI.create("/campaigns/" + response.getId())).body(response);
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @Valid @RequestBody CampaignUpdateRequest request,
            Authentication authentication) {
        CampaignResponse response = campaignService.updateCampaign(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{campaignId}/payment-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @PathVariable("campaignId") String campaignId,
            @Valid @RequestBody PaymentIntentRequest request) {
        PaymentIntentResponse response = stripeService.createPaymentIntent(campaignId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    // Direct donation path — used for testing and non-card flows; webhook is the production path
    @PostMapping("/{campaignId}/donate")
    public ResponseEntity<CampaignResponse> donate(
            @PathVariable("campaignId") String campaignId,
            @Valid @RequestBody DonationRequest request) {
        CampaignResponse response = campaignService.addDonation(campaignId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{campaignId}/close")
    public ResponseEntity<CampaignResponse> closeCampaign(
            @PathVariable("campaignId") String campaignId,
            Authentication authentication) {
        CampaignResponse response = campaignService.closeCampaign(campaignId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaignById(@PathVariable("campaignId") String campaignId) {
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }
}
