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

/** DynamoDB persistence layer for {@link UserRecord}. */
@Repository
public class UserDao {

    private static final String TABLE_NAME = "Users";

    private final DynamoDbTable<UserRecord> userTable;

    public UserDao(DynamoDbEnhancedClient enhancedClient) {
        this.userTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(UserRecord.class));
    }

    /**
     * Looks up a user by their partition key (ID).
     *
     * @param id the user ID
     * @return an Optional containing the record, or empty if not found
     */
    public Optional<UserRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(userTable.getItem(key));
    }

    /**
     * Persists a user record (insert or full replace).
     *
     * @param record the record to save
     * @return the saved record
     */
    public UserRecord save(UserRecord record) {
        userTable.putItem(record);
        return record;
    }

    /**
     * Deletes the user with the given ID. No-op if the item does not exist.
     *
     * @param id the user ID to delete
     */
    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        userTable.deleteItem(key);
    }

    /**
     * Returns {@code true} if a user with the given ID exists in the table.
     *
     * @param id the user ID
     * @return {@code true} if found
     */
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    /**
     * Finds a user by email address via a filtered full table scan.
     * This is O(n) — replace with a GSI-backed query when the table grows.
     *
     * @param email the email address to search for
     * @return an Optional containing the matching user, or empty if not found
     */
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
