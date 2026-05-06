package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.EventRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;

/** DAO for EventRecord persistence; replaces the legacy EventRepository. */
@Repository
public class EventDao {

    private static final String TABLE_NAME = "Events";

    private final DynamoDbTable<EventRecord> eventTable;

    public EventDao(DynamoDbEnhancedClient enhancedClient) {
        this.eventTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventRecord.class));
    }

    public Optional<EventRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(eventTable.getItem(key));
    }

    public EventRecord save(EventRecord record) {
        eventTable.putItem(record);
        return record;
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        eventTable.deleteItem(key);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public List<EventRecord> findAll() {
        return eventTable.scan().items().stream().toList();
    }
}
