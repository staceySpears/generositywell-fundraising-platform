package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.CampaignRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;

@Repository
public class CampaignDao {

    private static final String TABLE_NAME = "Campaigns";

    private final DynamoDbTable<CampaignRecord> campaignTable;

    public CampaignDao(DynamoDbEnhancedClient enhancedClient) {
        this.campaignTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(CampaignRecord.class));
    }

    public Optional<CampaignRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(campaignTable.getItem(key));
    }

    public CampaignRecord save(CampaignRecord record) {
        campaignTable.putItem(record);
        return record;
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        campaignTable.deleteItem(key);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public List<CampaignRecord> findAll() {
        return campaignTable.scan().items().stream().toList();
    }
}
