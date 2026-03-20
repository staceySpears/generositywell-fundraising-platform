package com.kenzie.capstone.service.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import com.kenzie.appserver.service.model.Customer;
import com.kenzie.appserver.service.model.User;

import java.util.List;

@DynamoDbBean
public class EventRecord {
    private String id;
    private String name;
    private String date;
    private User user;
    private List<Customer> listOfAttending;
    private String address;
    private String description;

    public EventRecord() {}

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

    @DynamoDbConvertedBy(CustomerTypeConverter.class)
    public List<Customer> getListOfAttending() {
        return listOfAttending;
    }

    public void setListOfAttending(List<Customer> listOfAttending) {
        this.listOfAttending = listOfAttending;
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

    @Override
    public String toString() {
        return super.toString();
    }
}


}
