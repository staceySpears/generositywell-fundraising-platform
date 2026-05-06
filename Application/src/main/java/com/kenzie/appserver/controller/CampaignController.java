package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
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
    public ResponseEntity<CampaignResponse> updateCampaign(@Valid @RequestBody CampaignUpdateRequest request) {
        CampaignResponse response = campaignService.updateCampaign(request);
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
