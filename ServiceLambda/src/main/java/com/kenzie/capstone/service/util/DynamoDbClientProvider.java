package com.kenzie.capstone.service.util;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Provides AWS SDK v2 DynamoDB clients.
     * Replaces the legacy SDK v1 AmazonDynamoDB / AmazonDynamoDBClientBuilder.
     */
public class DynamoDbClientProvider {

    /**
     * Returns a DynamoDbClient (low-level) for US_EAST_1.
         * @return DynamoDbClient
         */
    public static DynamoDbClient getDynamoDBClient() {
                return getDynamoDBClient(Region.US_EAST_1);
    }

    /**
     * Returns a DynamoDbClient (low-level) for the given region.
         * @param region AWS region
         * @return DynamoDbClient
         */
    public static DynamoDbClient getDynamoDBClient(Region region) {
                if (region == null) {
                                throw new IllegalArgumentException("region cannot be null");
                }
                return DynamoDbClient.builder()
                                    .region(region)
                                    .credentialsProvider(DefaultCredentialsProvider.create())
                                    .build();
    }

    /**
     * Returns a DynamoDbEnhancedClient wrapping the low-level client.
         * Used by the DAO layer with @DynamoDbBean annotated records.
         * @return DynamoDbEnhancedClient
         */
    public static DynamoDbEnhancedClient getEnhancedClient() {
                return DynamoDbEnhancedClient.builder()
                                    .dynamoDbClient(getDynamoDBClient())
                                    .build();
    }
}
