package com.kenzie.appserver;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockPropertySource;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

public class DynamoDbInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static GenericContainer<?> dynamoDb;

    private static GenericContainer<?> getDynamoDbInstance() {
        if (dynamoDb == null) {
            dynamoDb = new GenericContainer<>("amazon/dynamodb-local:latest")
                    .withExposedPorts(8000);
        }
        return dynamoDb;
    }

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        if (System.getenv("STACK_NAME") == null && System.getenv("ARTIFACT_BUCKET") == null) {
            getDynamoDbInstance().start();
            String endpoint = "http://localhost:" + getDynamoDbInstance().getMappedPort(8000);

            configurableApplicationContext.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MockPropertySource("dynamodb-initializer-properties")
                            .withProperty("dynamodb.override_endpoint", "true")
                            .withProperty("dynamodb.endpoint", endpoint));

            createTables(endpoint);
        }
    }

    private void createTables(String endpoint) {
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build()) {

            createTable(client, "Campaigns");
            createTable(client, "Users");
        }
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
