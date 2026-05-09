package com.kenzie.appserver.repositories;

import com.kenzie.appserver.repositories.model.AuditLogRecord;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Append-only DynamoDB persistence layer for {@link AuditLogRecord}.
 *
 * <p><strong>Immutability contract:</strong> this DAO intentionally exposes no
 * {@code delete} or {@code update} method. Audit records must never be modified
 * after they are written.
 *
 * <p>Both GSI queries return results in ascending timestamp order (oldest first).
 * To get the most recent events, reverse the returned list or use the
 * {@code scanIndexForward(false)} variant if you add a descending helper later.
 */
@Repository
public class AuditLogDao {

    private static final String TABLE_NAME = "AuditLog";

    private final DynamoDbTable<AuditLogRecord> auditTable;
    private final DynamoDbIndex<AuditLogRecord> entityHistoryIndex;
    private final DynamoDbIndex<AuditLogRecord> actorHistoryIndex;

    public AuditLogDao(DynamoDbEnhancedClient enhancedClient) {
        this.auditTable = enhancedClient.table(TABLE_NAME,
                TableSchema.fromBean(AuditLogRecord.class));
        this.entityHistoryIndex = auditTable.index(AuditLogRecord.ENTITY_HISTORY_INDEX);
        this.actorHistoryIndex = auditTable.index(AuditLogRecord.ACTOR_HISTORY_INDEX);
    }

    /**
     * Appends a new audit record. This is the only write operation this DAO exposes.
     *
     * @param record the record to persist
     */
    public void save(AuditLogRecord record) {
        auditTable.putItem(record);
    }

    /**
     * Returns all audit events for a specific entity, ordered by timestamp ascending.
     * Uses the {@value AuditLogRecord#ENTITY_HISTORY_INDEX} GSI.
     *
     * @param entityId the entity ID (campaign ID, event ID, etc.)
     * @return list of matching audit records, oldest first
     */
    public List<AuditLogRecord> findByEntityId(String entityId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(entityId).build());

        return entityHistoryIndex
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(condition)
                        .scanIndexForward(true)
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns all audit events triggered by a specific user, ordered by timestamp ascending.
     * Uses the {@value AuditLogRecord#ACTOR_HISTORY_INDEX} GSI.
     *
     * <p>This is the primary data source for the user Dashboard and the Salesforce
     * 360-degree supporter profile — it captures donations, RSVPs, campaign creation,
     * and every other action the user has taken across the entire platform.
     *
     * @param actorId the user ID
     * @return list of matching audit records, oldest first
     */
    public List<AuditLogRecord> findByActorId(String actorId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(actorId).build());

        return actorHistoryIndex
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(condition)
                        .scanIndexForward(true)
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }
}
