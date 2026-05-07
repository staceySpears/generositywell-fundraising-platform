package com.kenzie.appserver.repositories.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * DynamoDB-mapped record for a registered user.
 * {@code passwordHash} is a BCrypt hash and is never exposed in API responses.
 */
@DynamoDbBean
public class UserRecord {
    private String id;
    private String name;
    private String email;
    private String passwordHash;

    public UserRecord(){}
    public UserRecord(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /** @return the user's unique ID (partition key) */
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
    /** @param id the user ID */
    public void setId(String id) {
        this.id = id;
    }

    /** @return the user's display name */
    public String getName() {
        return name;
    }
    /** @param name the user's display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the user's email address */
    public String getEmail() {
        return email;
    }
    /** @param email the user's email address */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the BCrypt password hash; never include this in API responses */
    public String getPasswordHash() {
        return passwordHash;
    }
    /** @param passwordHash the BCrypt-encoded password */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
