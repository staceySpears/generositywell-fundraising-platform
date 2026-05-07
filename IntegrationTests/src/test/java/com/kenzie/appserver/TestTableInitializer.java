package com.kenzie.appserver;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Configuration
public class TestTableInitializer {

    @Bean
    ApplicationRunner createTestTables(DynamoDbClient dynamoDbClient) {
        return args -> {
            createTable(dynamoDbClient, "Campaigns");
            createTable(dynamoDbClient, "Users");
        };
    }

    private void createTable(DynamoDbClient client, String tableName) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException e) {
            // table already exists — fine when context is shared across test classes
        }
    }
}
