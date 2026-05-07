package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

/**
 * DynamoDB-mapped record for a fundraising campaign.
 * Monetary amounts are stored in cents (Long) to avoid floating-point rounding.
 * {@code status} holds a {@link com.kenzie.appserver.service.model.CampaignStatus} name.
 * {@code salesforceCampaignId} is populated asynchronously after the SF sync completes.
 */
@DynamoDbBean
public class CampaignRecord {

    private String id;
    private String name;
    private String date;
    private String deadline;
    private String category;
    private User user;
    private List<Supporter> supporters;
    private String address;
    private String description;
    private Long goalAmount;    // in cents
    private Long currentAmount; // in cents
    private String status;      // CampaignStatus name()
    private String salesforceCampaignId; // set async after SF sync

    public CampaignRecord() {}

    /** @return the campaign's unique ID (partition key) */
    @DynamoDbPartitionKey
    public String getId() { return id; }
    /** @param id the campaign ID */
    public void setId(String id) { this.id = id; }

    /** @return the campaign display name */
    public String getName() { return name; }
    /** @param name the campaign display name */
    public void setName(String name) { this.name = name; }

    /** @return the campaign start date (ISO-8601 string) */
    public String getDate() { return date; }
    /** @param date the campaign start date */
    public void setDate(String date) { this.date = date; }

    /** @return the fundraising deadline (ISO-8601 string) */
    public String getDeadline() { return deadline; }
    /** @param deadline the fundraising deadline */
    public void setDeadline(String deadline) { this.deadline = deadline; }

    /** @return the campaign category (e.g. "Community", "Education") */
    public String getCategory() { return category; }
    /** @param category the campaign category */
    public void setCategory(String category) { this.category = category; }

    /** @return the campaign owner; stored as JSON via {@link UserTypeConverter} */
    @DynamoDbConvertedBy(UserTypeConverter.class)
    public User getUser() { return user; }
    /** @param user the campaign owner */
    public void setUser(User user) { this.user = user; }

    /** @return the list of supporters; stored as JSON via {@link SupporterTypeConverter} */
    @DynamoDbConvertedBy(SupporterTypeConverter.class)
    public List<Supporter> getSupporters() { return supporters; }
    /** @param supporters the list of supporters */
    public void setSupporters(List<Supporter> supporters) { this.supporters = supporters; }

    /** @return the campaign's physical or virtual address */
    public String getAddress() { return address; }
    /** @param address the campaign address */
    public void setAddress(String address) { this.address = address; }

    /** @return the campaign description */
    public String getDescription() { return description; }
    /** @param description the campaign description */
    public void setDescription(String description) { this.description = description; }

    /** @return the fundraising goal in cents */
    public Long getGoalAmount() { return goalAmount; }
    /** @param goalAmount the fundraising goal in cents */
    public void setGoalAmount(Long goalAmount) { this.goalAmount = goalAmount; }

    /** @return the total amount raised so far, in cents */
    public Long getCurrentAmount() { return currentAmount; }
    /** @param currentAmount the total raised in cents */
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    /** @return the campaign status name (see {@link com.kenzie.appserver.service.model.CampaignStatus}) */
    public String getStatus() { return status; }
    /** @param status the campaign status name */
    public void setStatus(String status) { this.status = status; }

    /** @return the Salesforce Campaign ID, or {@code null} if the async sync has not completed */
    public String getSalesforceCampaignId() { return salesforceCampaignId; }
    /** @param salesforceCampaignId the Salesforce Campaign record ID */
    public void setSalesforceCampaignId(String salesforceCampaignId) { this.salesforceCampaignId = salesforceCampaignId; }
}
