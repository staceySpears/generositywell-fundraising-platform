package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.UserRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

/** DAO for UserRecord persistence; replaces the legacy EventUserRepository. */
@Repository
public class UserDao {

    private static final String TABLE_NAME = "Users";

    private final DynamoDbTable<UserRecord> userTable;

    public UserDao(DynamoDbEnhancedClient enhancedClient) {
        this.userTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(UserRecord.class));
    }

    public Optional<UserRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(userTable.getItem(key));
    }

    public UserRecord save(UserRecord record) {
        userTable.putItem(record);
        return record;
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        userTable.deleteItem(key);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    // Full scan filtered by email — replace with a GSI when table grows
    public Optional<UserRecord> findByEmail(String email) {
        Expression filter = Expression.builder()
                .expression("email = :email")
                .expressionValues(Map.of(":email", AttributeValue.builder().s(email).build()))
                .build();
        return userTable.scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items()
                .stream()
                .findFirst();
    }
}
