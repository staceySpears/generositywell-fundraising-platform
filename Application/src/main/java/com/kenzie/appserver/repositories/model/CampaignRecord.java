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
    private User user;
    private List<Supporter> supporters;
    private String address;
    private String description;

    public CampaignRecord() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @DynamoDbConvertedBy(UserTypeConverter.class)
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @DynamoDbConvertedBy(SupporterTypeConverter.class)
    public List<Supporter> getSupporters() {
        return supporters;
    }

    public void setSupporters(List<Supporter> supporters) {
        this.supporters = supporters;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
