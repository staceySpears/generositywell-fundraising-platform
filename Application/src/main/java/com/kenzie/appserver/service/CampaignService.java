package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.repositories.CampaignDao;
import com.kenzie.appserver.repositories.model.CampaignRecord;
import com.kenzie.appserver.salesforce.SalesforceService;
import com.kenzie.appserver.controller.model.DonationSummaryResponse;
import com.kenzie.appserver.service.model.AuditAction;
import com.kenzie.appserver.service.model.AuditEntityType;
import com.kenzie.appserver.service.model.CampaignStatus;
import com.kenzie.appserver.service.model.Supporter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Business logic for campaign lifecycle: creation, updates, donations, and closure. */
@Service
public class CampaignService {

    private final CampaignDao campaignDao;
    private final CacheStore<CampaignRecord> cache;
    private final SalesforceService salesforceService;
    private final AuditLogService auditLogService;

    /**
     * Constructs the service with its required collaborators.
     *
     * @param campaignDao       DynamoDB DAO for campaign persistence
     * @param cache             Caffeine-backed cache keyed by campaign ID
     * @param salesforceService async Salesforce CRM sync for campaigns and donations
     * @param auditLogService   append-only audit trail writer
     */
    public CampaignService(CampaignDao campaignDao,
                           @Qualifier("campaignCache") CacheStore<CampaignRecord> cache,
                           SalesforceService salesforceService,
                           AuditLogService auditLogService) {
        this.campaignDao = campaignDao;
        this.cache = cache;
        this.salesforceService = salesforceService;
        this.auditLogService = auditLogService;
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

        auditLogService.logAsync(AuditEntityType.CAMPAIGN, record.getId(),
                AuditAction.CAMPAIGN_CREATED, record.getUser().getId(),
                auditPayload("name", record.getName(), "goalAmount", record.getGoalAmount()));

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
        if (request == null || request.getId() == null || request.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID is required");
        }
        if (requestingUserId == null || requestingUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requesting user ID is required");
        }
        CampaignRecord record = campaignDao.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (record.getUser() == null || !Objects.equals(requestingUserId, record.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the campaign creator can update this campaign");
        }
        if (CampaignStatus.CLOSED.name().equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot update a closed campaign");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign name is required");
        }
        if (request.getGoalAmount() == null || request.getGoalAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Goal amount must be a positive value");
        }

        Long oldGoalAmount = record.getGoalAmount();
        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setDeadline(request.getDeadline());
        record.setCategory(request.getCategory());
        // Deliberately do NOT call record.setUser() — the persisted owner is immutable
        // after creation. The ownership check above already ensures only the original
        // creator reaches this point, so the stored user field is already correct.
        record.setSupporters(request.getSupporters());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        boolean goalChanged = !Objects.equals(oldGoalAmount, request.getGoalAmount());
        record.setGoalAmount(request.getGoalAmount());
        campaignDao.save(record);
        cache.evict(record.getId());

        if (goalChanged) {
            auditLogService.logAsync(AuditEntityType.CAMPAIGN, record.getId(),
                    AuditAction.GOAL_UPDATED, requestingUserId,
                    auditPayload("oldGoal", oldGoalAmount, "newGoal", request.getGoalAmount()));
        } else {
            auditLogService.logAsync(AuditEntityType.CAMPAIGN, record.getId(),
                    AuditAction.CAMPAIGN_UPDATED, requestingUserId,
                    auditPayload("name", record.getName()));
        }

        return recordToResponse(record);
    }

    /**
     * Records a donation against a campaign and flips status to FUNDED when the goal is met.
     * Triggers an async Salesforce Opportunity sync after saving.
     * When {@code donorId} is non-null the donor's cumulative total is tracked in the
     * campaign's {@code supporters} list so giving history can be queried later.
     *
     * @param campaignId    the campaign to donate to
     * @param amountInCents the donation amount in cents (must be a positive value)
     * @param donorId       the authenticated donor's user ID, or {@code null} for webhook/anonymous paths
     * @return the updated campaign response
     * @throws org.springframework.web.server.ResponseStatusException 400 if campaignId is blank
     *         or amountInCents is null, zero, or negative; 404 if not found;
     *         409 if the campaign is already closed
     */
    public CampaignResponse addDonation(String campaignId, Long amountInCents, String donorId) {
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

        // Track the donor in the supporters list when a userId is available.
        if (donorId != null) {
            List<Supporter> supporters = record.getSupporters() != null
                    ? new ArrayList<>(record.getSupporters()) : new ArrayList<>();
            Supporter existing = supporters.stream()
                    .filter(s -> donorId.equals(s.getId()))
                    .findFirst().orElse(null);
            if (existing != null) {
                existing.setAmountInCents(
                        (existing.getAmountInCents() != null ? existing.getAmountInCents() : 0L) + amountInCents);
                existing.setDonationDate(LocalDate.now().toString());
            } else {
                Supporter supporter = new Supporter();
                supporter.setId(donorId);
                supporter.setAmountInCents(amountInCents);
                supporter.setDonationDate(LocalDate.now().toString());
                supporters.add(supporter);
            }
            record.setSupporters(supporters);
        }

        campaignDao.save(record);
        cache.evict(campaignId);
        salesforceService.syncDonation(record, amountInCents);

        // Synchronous: the donation must be in the audit log before the response is sent.
        // actorId is unknown here (Stripe webhook path has no authenticated user);
        // use the campaign owner as the contextual actor for the ledger record.
        String actorId = record.getUser() != null ? record.getUser().getId() : "system";
        auditLogService.logSync(AuditEntityType.DONATION, campaignId,
                AuditAction.PAYMENT_SUCCEEDED, actorId,
                auditPayload("amountInCents", amountInCents, "newTotalCents", newTotal,
                        "status", record.getStatus()));

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

        auditLogService.logAsync(AuditEntityType.CAMPAIGN, campaignId,
                AuditAction.CAMPAIGN_CLOSED, requestingUserId,
                auditPayload("finalStatus", CampaignStatus.CLOSED.name()));

        return recordToResponse(record);
    }

    /**
     * Permanently deletes a campaign and evicts it from the cache.
     *
     * @param campaignId       the campaign to delete
     * @param requestingUserId the user ID from the authenticated JWT
     * @throws org.springframework.web.server.ResponseStatusException 400 if ID is blank,
     *         404 if not found, 403 if the caller is not the owner
     */
    public void deleteCampaign(String campaignId, String requestingUserId) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        CampaignRecord record = campaignDao.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (record.getUser() == null || !Objects.equals(requestingUserId, record.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the campaign creator can delete this campaign");
        }

        campaignDao.deleteById(campaignId);
        cache.evict(campaignId);

        auditLogService.logAsync(AuditEntityType.CAMPAIGN, campaignId,
                AuditAction.CAMPAIGN_DELETED, requestingUserId,
                auditPayload("campaignId", campaignId));
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

    /**
     * Returns all campaigns created by the given user, sorted by deadline descending
     * (most recent first, nulls last).
     *
     * @param userId the creator's user ID
     * @return list of campaign responses owned by that user
     * @throws ResponseStatusException 400 if userId is blank
     */
    public List<CampaignResponse> getCampaignsByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID cannot be empty");
        }
        return campaignDao.findByUserId(userId).stream()
                .map(this::recordToResponse)
                .toList();
    }

    /**
     * Returns a giving-history summary for the given user — one entry per campaign
     * where they have a tracked donation.
     *
     * @param userId the donor's user ID
     * @return list of donation summaries, newest donation date first
     * @throws ResponseStatusException 400 if userId is blank
     */
    public List<DonationSummaryResponse> getDonationsByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID cannot be empty");
        }
        return campaignDao.findByDonorId(userId).stream()
                .map(record -> {
                    Supporter entry = record.getSupporters().stream()
                            .filter(s -> userId.equals(s.getId()))
                            .findFirst().orElse(null);
                    DonationSummaryResponse summary = new DonationSummaryResponse();
                    summary.setCampaignId(record.getId());
                    summary.setCampaignName(record.getName());
                    summary.setAmountInCents(entry != null ? entry.getAmountInCents() : null);
                    summary.setDonationDate(entry != null ? entry.getDonationDate() : null);
                    return summary;
                })
                .sorted((a, b) -> {
                    // Most recent donation date first; nulls last
                    if (a.getDonationDate() == null && b.getDonationDate() == null) return 0;
                    if (a.getDonationDate() == null) return 1;
                    if (b.getDonationDate() == null) return -1;
                    return b.getDonationDate().compareTo(a.getDonationDate());
                })
                .toList();
    }

    /**
     * Maps a {@link CampaignRecord} to a {@link CampaignResponse}, computing
     * {@code percentFunded} when both goal and current amounts are available.
     *
     * @param record the persisted campaign record
     * @return the API response view of the campaign
     */
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

    /**
     * Builds a null-safe payload map for audit log entries.
     * Unlike {@link Map#of}, this helper accepts {@code null} values — important for
     * nullable fields such as {@code Long goalAmount} that may not yet be set on a record.
     *
     * @param keysAndValues alternating key (String) / value (Object) pairs
     * @return a mutable HashMap containing the provided pairs
     */
    private static Map<String, Object> auditPayload(Object... keysAndValues) {
        if (keysAndValues == null || keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("auditPayload requires an even number of alternating key/value pairs");
        }
        Map<String, Object> map = new HashMap<>(keysAndValues.length / 2);
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            if (!(keysAndValues[i] instanceof String key)) {
                throw new IllegalArgumentException("auditPayload key at index " + i + " must be a String");
            }
            map.put(key, keysAndValues[i + 1]);
        }
        return map;
    }
}
