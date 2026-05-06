package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.UserRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

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
}
