package com.kenzie.appserver.repositories.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.time.Instant;

/**
 * Append-only DynamoDB record representing a single audited event.
 *
 * <h3>Table design</h3>
 * <ul>
 *   <li>Partition key: {@code logId} (UUID) — prevents hot partitions on busy entities</li>
 *   <li>GSI {@value #ENTITY_HISTORY_INDEX}: {@code entityId} PK + {@code timestamp} SK —
 *       query "all events for Campaign X, newest first"</li>
 *   <li>GSI {@value #ACTOR_HISTORY_INDEX}: {@code actorId} PK + {@code timestamp} SK —
 *       query "everything user Y has ever done" for the Dashboard and Salesforce 360 profile</li>
 * </ul>
 *
 * <h3>Timestamp storage</h3>
 * {@link Instant} is serialised by the Enhanced Client as an ISO-8601 string
 * (e.g. {@code "2026-05-09T12:34:56.789Z"}), which sorts correctly as a DynamoDB
 * sort-key attribute of type {@code S} — no epoch-millisecond workaround needed.
 *
 * <h3>Immutability contract</h3>
 * Records must never be updated or deleted. The {@code AuditLogDao} exposes only
 * {@code save} and query operations — no {@code delete} or {@code update}.
 */
@DynamoDbBean
public class AuditLogRecord {

    /** Name of the GSI for entity-scoped history queries. */
    public static final String ENTITY_HISTORY_INDEX = "EntityHistoryIndex";

    /** Name of the GSI for actor-scoped history queries. */
    public static final String ACTOR_HISTORY_INDEX = "ActorHistoryIndex";

    private String logId;
    private String entityType;  // AuditEntityType.name()
    private String entityId;
    private String action;      // AuditAction.name()
    private String actorId;
    private Instant timestamp;
    private String payload;     // JSON delta — what changed and to what value

    public AuditLogRecord() {}

    /** @return the unique log entry ID (partition key) */
    @DynamoDbPartitionKey
    public String getLogId() { return logId; }
    /** @param logId the unique log entry ID */
    public void setLogId(String logId) { this.logId = logId; }

    /** @return the entity type name (see {@link com.kenzie.appserver.service.model.AuditEntityType}) */
    public String getEntityType() { return entityType; }
    /** @param entityType the entity type name */
    public void setEntityType(String entityType) { this.entityType = entityType; }

    /**
     * @return the ID of the entity this log entry describes;
     *         also the partition key of {@value #ENTITY_HISTORY_INDEX}
     */
    @DynamoDbSecondaryPartitionKey(indexNames = ENTITY_HISTORY_INDEX)
    public String getEntityId() { return entityId; }
    /** @param entityId the entity ID */
    public void setEntityId(String entityId) { this.entityId = entityId; }

    /** @return the action name (see {@link com.kenzie.appserver.service.model.AuditAction}) */
    public String getAction() { return action; }
    /** @param action the action name */
    public void setAction(String action) { this.action = action; }

    /**
     * @return the user ID who triggered this event;
     *         also the partition key of {@value #ACTOR_HISTORY_INDEX}
     */
    @DynamoDbSecondaryPartitionKey(indexNames = ACTOR_HISTORY_INDEX)
    public String getActorId() { return actorId; }
    /** @param actorId the acting user ID */
    public void setActorId(String actorId) { this.actorId = actorId; }

    /**
     * @return the event timestamp (ISO-8601 string in DynamoDB);
     *         sort key for both {@value #ENTITY_HISTORY_INDEX} and {@value #ACTOR_HISTORY_INDEX}
     */
    @DynamoDbSecondarySortKey(indexNames = { ENTITY_HISTORY_INDEX, ACTOR_HISTORY_INDEX })
    public Instant getTimestamp() { return timestamp; }
    /** @param timestamp the event timestamp */
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    /**
     * @return a JSON string describing what changed — shape varies by action
     *         (e.g. {@code {"oldGoal":5000,"newGoal":10000}} for {@code GOAL_UPDATED})
     */
    public String getPayload() { return payload; }
    /** @param payload the JSON delta payload */
    public void setPayload(String payload) { this.payload = payload; }
}
