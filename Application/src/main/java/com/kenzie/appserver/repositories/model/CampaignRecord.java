package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Supporter;
import com.kenzie.appserver.service.model.User;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

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

    public CampaignRecord() {}

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @DynamoDbConvertedBy(UserTypeConverter.class)
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @DynamoDbConvertedBy(SupporterTypeConverter.class)
    public List<Supporter> getSupporters() { return supporters; }
    public void setSupporters(List<Supporter> supporters) { this.supporters = supporters; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getGoalAmount() { return goalAmount; }
    public void setGoalAmount(Long goalAmount) { this.goalAmount = goalAmount; }

    public Long getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
