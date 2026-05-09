package com.kenzie.appserver.salesforce;

import com.kenzie.appserver.repositories.CampaignDao;
import com.kenzie.appserver.repositories.model.CampaignRecord;
import com.kenzie.appserver.repositories.model.UserRecord;
import com.kenzie.appserver.service.model.CampaignStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Asynchronous bridge between GenerosityWell and Salesforce NPSP.
 * All public methods are {@code @Async} and are no-ops when {@code salesforce.enabled=false}.
 * Failures are caught and logged rather than propagated, so a Salesforce outage never
 * breaks a user-facing request.
 */
@Service
public class SalesforceService {

    private static final Logger log = LoggerFactory.getLogger(SalesforceService.class);

    private final SalesforceClient client;
    private final CampaignDao campaignDao;
    private final boolean enabled;

    public SalesforceService(
            SalesforceClient client,
            CampaignDao campaignDao,
            @Value("${salesforce.enabled:false}") boolean enabled) {
        this.client = client;
        this.campaignDao = campaignDao;
        this.enabled = enabled;
    }

    /**
     * Creates a Salesforce Campaign record for a newly saved local campaign.
     * After the SF campaign is created, re-fetches the latest local record before
     * writing back the SF ID to avoid overwriting concurrent updates (e.g. donations
     * that arrived while this async task was queued).
     *
     * @param record a snapshot of the CampaignRecord at the time the campaign was created
     */
    @Async
    public void syncCampaign(CampaignRecord record) {
        if (!enabled) return;
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("Name", record.getName());
            fields.put("Description", record.getDescription());
            fields.put("StartDate", record.getDate() != null ? record.getDate().toString() : null);
            fields.put("EndDate", record.getDeadline() != null ? record.getDeadline().toString() : null);
            fields.put("Type", "Fundraising");
            fields.put("Status", toSalesforceCampaignStatus(record.getStatus()));
            fields.put("ExpectedRevenue", record.getGoalAmount() / 100.0);
            fields.put("ActualCost", record.getCurrentAmount() / 100.0);
            fields.put("IsActive", !CampaignStatus.CLOSED.name().equals(record.getStatus()));

            String sfId = client.create("Campaign", fields);

            // Re-fetch before writing back to avoid overwriting concurrent updates (donations, status changes)
            campaignDao.findById(record.getId()).ifPresentOrElse(latest -> {
                latest.setSalesforceCampaignId(sfId);
                campaignDao.save(latest);
                log.info("Campaign synced to Salesforce: local={} sf={}", latest.getId(), sfId);
            }, () -> log.warn("Skipping SF campaign ID persist; local campaign not found: {}", record.getId()));

        } catch (Exception e) {
            log.error("Failed to sync campaign {} to Salesforce: {}", record.getId(), e.getMessage());
        }
    }

    /**
     * Creates a Salesforce Opportunity (NPSP donation record) for a recorded donation.
     * If the local campaign's Salesforce ID has not been persisted yet (because
     * {@link #syncCampaign} is still in-flight), the Opportunity will be created
     * without a {@code CampaignId} link — this is logged as a warning.
     *
     * @param record        the campaign record at the time of the donation
     * @param amountInCents the donation amount in cents
     */
    @Async
    public void syncDonation(CampaignRecord record, Long amountInCents) {
        if (!enabled) return;
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("Name", "Donation to " + record.getName());
            fields.put("Amount", amountInCents / 100.0);
            fields.put("CloseDate", LocalDate.now().toString());
            fields.put("StageName", "Closed Won");
            fields.put("Type", "Donation");

            if (record.getSalesforceCampaignId() != null) {
                fields.put("CampaignId", record.getSalesforceCampaignId());
            } else {
                log.warn("Donation Opportunity created without CampaignId — SF campaign not yet synced: local={}", record.getId());
            }

            String sfId = client.create("Opportunity", fields);
            log.info("Donation synced to Salesforce: opportunity={} campaign={} amount={}",
                    sfId, record.getId(), amountInCents);

        } catch (Exception e) {
            log.error("Failed to sync donation for campaign {} to Salesforce: {}", record.getId(), e.getMessage());
        }
    }

    /**
     * Creates a Salesforce Contact record for a newly registered user.
     * Single-name users (no space) are stored with an empty first name and the
     * full name as the last name.
     *
     * @param record the UserRecord of the newly registered user
     */
    @Async
    public void syncContact(UserRecord record) {
        if (!enabled) return;
        try {
            String[] nameParts = record.getName().split(" ", 2);
            String firstName = nameParts.length > 1 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : nameParts[0];

            Map<String, Object> fields = new HashMap<>();
            fields.put("FirstName", firstName);
            fields.put("LastName", lastName);
            fields.put("Email", record.getEmail());

            String sfId = client.create("Contact", fields);
            log.info("Contact synced to Salesforce: local={} sf={}", record.getId(), sfId);

        } catch (Exception e) {
            log.error("Failed to sync contact {} to Salesforce: {}", record.getId(), e.getMessage());
        }
    }

    private String toSalesforceCampaignStatus(String status) {
        if (CampaignStatus.ACTIVE.name().equals(status)) return "Active";
        if (CampaignStatus.FUNDED.name().equals(status)) return "Active";
        if (CampaignStatus.CLOSED.name().equals(status)) return "Completed";
        return "Planned";
    }
}
