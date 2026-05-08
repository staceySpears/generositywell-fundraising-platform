package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.repositories.CampaignDao;
import com.kenzie.appserver.repositories.model.CampaignRecord;
import com.kenzie.appserver.salesforce.SalesforceService;
import com.kenzie.appserver.service.model.CampaignStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Business logic for campaign lifecycle: creation, updates, donations, and closure. */
@Service
public class CampaignService {

    private final CampaignDao campaignDao;
    private final CacheStore cache;
    private final SalesforceService salesforceService;

    public CampaignService(CampaignDao campaignDao, CacheStore cache, SalesforceService salesforceService) {
        this.campaignDao = campaignDao;
        this.cache = cache;
        this.salesforceService = salesforceService;
    }

    /**
     * Returns the campaign with the given ID.
     * Results are served from the in-memory cache when available.
     * Cache misses (empty Optionals) are also cached to avoid repeated DynamoDB calls.
     *
     * @param id the campaign ID
     * @return the campaign response
     * @throws org.springframework.web.server.ResponseStatusException 400 if ID is blank, 404 if not found
     */
    public CampaignResponse getCampaignById(String id) {
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        Optional<CampaignRecord> cached = cache.get(id);
        if (cached != null) {
            return cached.map(this::recordToResponse)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        }
        Optional<CampaignRecord> record = campaignDao.findById(id);
        cache.add(id, record);
        return record.map(this::recordToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    /**
     * Creates a new campaign with ACTIVE status and zero current amount.
     * Triggers an async Salesforce Campaign sync after the local record is saved.
     *
     * @param request the creation request
     * @return the created campaign response
     * @throws org.springframework.web.server.ResponseStatusException 400 if name is blank,
     *         goalAmount is null or non-positive, or user is null
     */
    public CampaignResponse addNewCampaign(CreateCampaignRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign name is required");
        }
        if (request.getGoalAmount() == null || request.getGoalAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Goal amount must be a positive value");
        }
        if (request.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign must have an owner");
        }

        CampaignRecord record = new CampaignRecord();
        record.setId(UUID.randomUUID().toString());
        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setDeadline(request.getDeadline());
        record.setCategory(request.getCategory());
        record.setUser(request.getUser());
        record.setSupporters(request.getSupporters());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        record.setGoalAmount(request.getGoalAmount());
        record.setCurrentAmount(0L);
        record.setStatus(CampaignStatus.ACTIVE.name());
        campaignDao.save(record);
        salesforceService.syncCampaign(record);

        return recordToResponse(record);
    }

    /**
     * Updates a campaign's mutable fields.
     * Only the original creator may update; ownership is verified against the JWT subject.
     *
     * @param request          the update request
     * @param requestingUserId the user ID from the authenticated JWT
     * @return the updated campaign response
     * @throws org.springframework.web.server.ResponseStatusException 404 if not found,
     *         403 if the caller is not the owner, 409 if the campaign is closed
     */
    public CampaignResponse updateCampaign(CampaignUpdateRequest request, String requestingUserId) {
        CampaignRecord record = campaignDao.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (record.getUser() == null || !Objects.equals(requestingUserId, record.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the campaign creator can update this campaign");
        }
        if (CampaignStatus.CLOSED.name().equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot update a closed campaign");
        }

        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setDeadline(request.getDeadline());
        record.setCategory(request.getCategory());
        record.setUser(request.getUser());
        record.setSupporters(request.getSupporters());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        record.setGoalAmount(request.getGoalAmount());
        campaignDao.save(record);
        cache.evict(record.getId());

        return recordToResponse(record);
    }

    /**
     * Records a donation against a campaign and flips status to FUNDED when the goal is met.
     * Triggers an async Salesforce Opportunity sync after saving.
     *
     * @param campaignId    the campaign to donate to
     * @param amountInCents the donation amount in cents (must be a positive value)
     * @return the updated campaign response
     * @throws org.springframework.web.server.ResponseStatusException 400 if campaignId is blank
     *         or amountInCents is null, zero, or negative; 404 if not found;
     *         409 if the campaign is already closed
     */
    public CampaignResponse addDonation(String campaignId, Long amountInCents) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        CampaignRecord record = campaignDao.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (CampaignStatus.CLOSED.name().equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This campaign is no longer accepting donations");
        }

        if (amountInCents == null || amountInCents <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Donation amount must be a positive value");
        }

        long current = record.getCurrentAmount() != null ? record.getCurrentAmount() : 0L;
        long newTotal = current + amountInCents;
        record.setCurrentAmount(newTotal);

        if (newTotal >= record.getGoalAmount() && CampaignStatus.ACTIVE.name().equals(record.getStatus())) {
            record.setStatus(CampaignStatus.FUNDED.name());
        }

        campaignDao.save(record);
        cache.evict(campaignId);
        salesforceService.syncDonation(record, amountInCents);

        return recordToResponse(record);
    }

    /**
     * Closes a campaign, preventing further donations.
     * Only the original creator may close it.
     *
     * @param campaignId       the campaign to close
     * @param requestingUserId the user ID from the authenticated JWT
     * @return the updated campaign response with CLOSED status
     * @throws org.springframework.web.server.ResponseStatusException 400 if campaignId is blank,
     *         404 if not found, 403 if the caller is not the owner
     */
    public CampaignResponse closeCampaign(String campaignId, String requestingUserId) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        CampaignRecord record = campaignDao.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (record.getUser() == null || !Objects.equals(requestingUserId, record.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the campaign creator can close this campaign");
        }

        record.setStatus(CampaignStatus.CLOSED.name());
        campaignDao.save(record);
        cache.evict(campaignId);

        return recordToResponse(record);
    }

    /**
     * Permanently deletes a campaign and evicts it from the cache.
     *
     * @param campaignId the campaign to delete
     * @throws org.springframework.web.server.ResponseStatusException 400 if ID is blank,
     *         404 if not found
     */
    public void deleteCampaign(String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        if (!campaignDao.existsById(campaignId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found");
        }
        campaignDao.deleteById(campaignId);
        cache.evict(campaignId);
    }

    /**
     * Returns all campaigns. Performs a full DynamoDB table scan — prefer filtered queries at scale.
     *
     * @return list of all campaign responses
     */
    public List<CampaignResponse> getAllCampaigns() {
        return campaignDao.findAll().stream()
                .map(this::recordToResponse)
                .toList();
    }

    private CampaignResponse recordToResponse(CampaignRecord record) {
        CampaignResponse response = new CampaignResponse();
        response.setId(record.getId());
        response.setName(record.getName());
        response.setDate(record.getDate());
        response.setDeadline(record.getDeadline());
        response.setCategory(record.getCategory());
        response.setUser(record.getUser());
        response.setSupporters(record.getSupporters());
        response.setAddress(record.getAddress());
        response.setDescription(record.getDescription());
        response.setGoalAmount(record.getGoalAmount());
        response.setCurrentAmount(record.getCurrentAmount());
        response.setStatus(record.getStatus());

        if (record.getGoalAmount() != null && record.getGoalAmount() > 0 && record.getCurrentAmount() != null) {
            int pct = (int) (record.getCurrentAmount() * 100 / record.getGoalAmount());
            response.setPercentFunded(pct);
        }

        return response;
    }
}
