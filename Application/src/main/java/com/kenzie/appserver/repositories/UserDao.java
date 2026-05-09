package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.UserRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Optional;

/** DynamoDB persistence layer for {@link UserRecord}. */
@Repository
public class UserDao {

    private static final String TABLE_NAME = "users";
    private static final String EMAIL_INDEX = "email-index";

    private final DynamoDbTable<UserRecord> userTable;
    private final DynamoDbIndex<UserRecord> emailIndex;

    public UserDao(DynamoDbEnhancedClient enhancedClient) {
        this.userTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(UserRecord.class));
        this.emailIndex = userTable.index(EMAIL_INDEX);
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
     * Finds a user by email address using the {@code email-index} GSI.
     * O(1) DynamoDB query — replaces the previous O(n) full table scan.
     * Requires the {@code email-index} GSI to exist on the {@code users} table
     * (see {@code UsersTable.yml}).
     *
     * @param email the email address to search for
     * @return an Optional containing the matching user, or empty if not found
     */
    public Optional<UserRecord> findByEmail(String email) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(email).build());
        return emailIndex.query(condition).stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }
}
