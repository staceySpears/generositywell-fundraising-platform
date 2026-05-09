package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.CampaignRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** DynamoDB persistence layer for {@link CampaignRecord}. */
@Repository
public class CampaignDao {

    private static final String TABLE_NAME = "Campaigns";

    private final DynamoDbTable<CampaignRecord> campaignTable;

    public CampaignDao(DynamoDbEnhancedClient enhancedClient) {
        this.campaignTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(CampaignRecord.class));
    }

    /**
     * Looks up a campaign by its partition key.
     *
     * @param id the campaign ID
     * @return an Optional containing the record, or empty if not found
     */
    public Optional<CampaignRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(campaignTable.getItem(key));
    }

    /**
     * Persists a campaign record (insert or full replace).
     *
     * @param record the record to save
     * @return the saved record
     */
    public CampaignRecord save(CampaignRecord record) {
        campaignTable.putItem(record);
        return record;
    }

    /**
     * Deletes the campaign with the given ID. No-op if the item does not exist.
     *
     * @param id the campaign ID to delete
     */
    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        campaignTable.deleteItem(key);
    }

    /**
     * Returns {@code true} if a campaign with the given ID exists in the table.
     *
     * @param id the campaign ID
     * @return {@code true} if found
     */
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    /**
     * Returns all campaigns via a full table scan.
     * Avoid at large data volumes — prefer paginated or filtered queries.
     *
     * @return list of all campaign records
     */
    public List<CampaignRecord> findAll() {
        return campaignTable.scan().items().stream().collect(Collectors.toList());
    }

    /**
     * Returns all campaigns created by the given user.
     * Uses a client-side filter over a full table scan.
     * Add a {@code creatorId} GSI at scale to replace this with an index query.
     *
     * @param userId the creator's user ID
     * @return list of campaigns owned by that user
     */
    public List<CampaignRecord> findByUserId(String userId) {
        return findAll().stream()
                .filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()))
                .collect(Collectors.toList());
    }
}
