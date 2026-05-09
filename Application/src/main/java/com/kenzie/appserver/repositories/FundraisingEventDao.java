package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.FundraisingEventRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** DynamoDB persistence layer for {@link FundraisingEventRecord}. */
@Repository
public class FundraisingEventDao {

    private static final String TABLE_NAME = "FundraisingEvents";

    private final DynamoDbTable<FundraisingEventRecord> eventTable;

    public FundraisingEventDao(DynamoDbEnhancedClient enhancedClient) {
        this.eventTable = enhancedClient.table(TABLE_NAME,
                TableSchema.fromBean(FundraisingEventRecord.class));
    }

    /**
     * Looks up an event by its partition key.
     *
     * @param id the event ID
     * @return an Optional containing the record, or empty if not found
     */
    public Optional<FundraisingEventRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(eventTable.getItem(key));
    }

    /**
     * Persists an event record (insert or full replace).
     *
     * @param record the record to save
     * @return the saved record
     */
    public FundraisingEventRecord save(FundraisingEventRecord record) {
        eventTable.putItem(record);
        return record;
    }

    /**
     * Deletes the event with the given ID. No-op if the item does not exist.
     *
     * @param id the event ID to delete
     */
    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        eventTable.deleteItem(key);
    }

    /**
     * Returns {@code true} if an event with the given ID exists in the table.
     *
     * @param id the event ID
     * @return {@code true} if found
     */
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    /**
     * Returns all events via a full table scan.
     * Avoid at large data volumes — prefer paginated or filtered queries.
     *
     * @return list of all event records
     */
    public List<FundraisingEventRecord> findAll() {
        return eventTable.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    /**
     * Returns all events associated with a given campaign.
     * Uses a full table scan with client-side filter — add a GSI on {@code campaignId} at scale.
     *
     * @param campaignId the campaign ID to filter by
     * @return list of matching event records
     */
    public List<FundraisingEventRecord> findByCampaignId(String campaignId) {
        return findAll().stream()
                .filter(r -> campaignId.equals(r.getCampaignId()))
                .collect(Collectors.toList());
    }
}
