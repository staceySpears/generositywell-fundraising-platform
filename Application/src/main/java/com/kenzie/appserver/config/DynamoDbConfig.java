package com.kenzie.appserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/** Spring configuration for AWS DynamoDB client beans. */
@Configuration
public class DynamoDbConfig {

    /**
     * Local DynamoDB client that overrides the endpoint — active when
     * {@code dynamodb.override_endpoint=true} (e.g. in integration tests using Testcontainers).
     *
     * @param dynamoEndpoint the local endpoint URL (e.g. {@code http://localhost:8000})
     * @return a DynamoDbClient pointed at the local endpoint
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "dynamodb.override_endpoint", havingValue = "true")
    public DynamoDbClient localDynamoDbClient(@Value("${dynamodb.endpoint}") String dynamoEndpoint) {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Production DynamoDB client using the default AWS credentials chain and region from the environment.
     *
     * @return a DynamoDbClient configured for production use
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Enhanced DynamoDB client that wraps the base client and provides the bean-mapped API.
     *
     * @param dynamoDbClient the base DynamoDB client (local or production depending on active config)
     * @return a DynamoDbEnhancedClient backed by the provided client
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
