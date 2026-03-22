package com.kenzie.capstone.service.dao;

import com.kenzie.capstone.service.model.EventRecord;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DAO for EventRecord persistence via AWS SDK v2 DynamoDB Enhanced Client.
     * Replaces the legacy SDK v1 DynamoDBMapper implementation.
     */
public class EventDao {

    private static final String TABLE_NAME = "Events";

    private final DynamoDbTable<EventRecord> eventTable;

    public EventDao(DynamoDbEnhancedClient enhancedClient) {
                this.eventTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventRecord.class));
    }

    /**
     * Retrieves all EventRecords matching the given partition key (id).
         * @param id the event ID to query
         * @return list of matching EventRecords
         */
    public List<EventRecord> getEventById(String id) {
                Key key = Key.builder().partitionValue(id).build();

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                                .queryConditional(QueryConditional.keyEqualTo(key))
                                .consistentRead(false)
                                .build();

            return eventTable.query(queryRequest)
                                .items()
                                .stream()
                                .collect(Collectors.toList());
    }

    /**
     * Persists a new EventRecord. Throws IllegalArgumentException if the id already exists.
         * @param record the event to persist
         * @return the persisted EventRecord
         */
    public EventRecord postNewEvent(EventRecord record) {
                try {
                                eventTable.putItem(
                                                    PutItemEnhancedRequest.builder(EventRecord.class)
                                                        .item(record)
                                                        .conditionExpression(
                                                                                    software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                                                                                        .expression("attribute_not_exists(id)")
                                                                                        .build()
                                                                                )
                                                        .build()
                                                );
                } catch (ConditionalCheckFailedException e) {
                                throw new IllegalArgumentException("id already exists");
                }
                return record;
    }

    /**
     * Scans and returns all EventRecords in the table.
         * @return list of all EventRecords
         */
    public List<EventRecord> getAllEvents() {
                return eventTable.scan()
                                    .items()
                                    .stream()
                                    .collect(Collectors.toList());
    }
}
