package com.kenzie.appserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kenzie.appserver.repositories.AuditLogDao;
import com.kenzie.appserver.repositories.model.AuditLogRecord;
import com.kenzie.appserver.service.model.AuditAction;
import com.kenzie.appserver.service.model.AuditEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit logging service.
 *
 * <p>Provides two write paths:
 * <ul>
 *   <li>{@link #logSync} — blocks the calling thread until the record is persisted.
 *       Use this for financial events ({@code PAYMENT_SUCCEEDED}, {@code PAYMENT_FAILED})
 *       where the audit entry must be durable before the HTTP response is sent.</li>
 *   <li>{@link #logAsync} — annotated with {@code @Async}; returns immediately and writes
 *       on a background thread from the application's executor pool. Use this for
 *       operational events (campaign created, RSVP confirmed, etc.) where a small write
 *       delay is acceptable and you don't want to slow the API response.</li>
 * </ul>
 *
 * <p>Async failures are logged at ERROR level but not propagated — a failed audit write
 * must never crash the business operation that triggered it.
 *
 * <p>Sync failures are propagated as {@link AuditLogException} — the caller decides
 * whether to roll back (financial events should treat an un-logged payment as suspicious).
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogDao auditLogDao;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogDao auditLogDao, ObjectMapper objectMapper) {
        this.auditLogDao = auditLogDao;
        this.objectMapper = objectMapper;
    }

    // ── Public write API ───────────────────────────────────────────────────────

    /**
     * Writes an audit record synchronously. The calling thread blocks until DynamoDB
     * confirms the write.
     *
     * <p>Use for financial events where durability must be confirmed before the
     * HTTP response is returned to the client.
     *
     * @param entityType the category of the affected entity
     * @param entityId   the ID of the affected entity
     * @param action     the action that occurred
     * @param actorId    the user ID who triggered the action
     * @param payload    an object describing the state delta; serialised to JSON
     * @throws AuditLogException if the payload cannot be serialised or the DynamoDB write fails
     */
    public void logSync(AuditEntityType entityType,
                        String entityId,
                        AuditAction action,
                        String actorId,
                        Object payload) {
        persist(entityType, entityId, action, actorId, payload);
    }

    /**
     * Writes an audit record asynchronously on a background thread.
     * Returns immediately; any write failure is logged but not propagated.
     *
     * <p>Use for operational events (campaign created, RSVP confirmed, etc.)
     * where a brief write delay is acceptable.
     *
     * @param entityType the category of the affected entity
     * @param entityId   the ID of the affected entity
     * @param action     the action that occurred
     * @param actorId    the user ID who triggered the action
     * @param payload    an object describing the state delta; serialised to JSON
     */
    @Async
    public void logAsync(AuditEntityType entityType,
                         String entityId,
                         AuditAction action,
                         String actorId,
                         Object payload) {
        try {
            persist(entityType, entityId, action, actorId, payload);
        } catch (AuditLogException e) {
            // Async audit failures must never propagate — log and continue.
            log.error("Async audit log write failed: entityType={} entityId={} action={} actorId={}",
                    entityType, entityId, action, actorId, e);
        }
    }

    // ── Core write ─────────────────────────────────────────────────────────────

    private void persist(AuditEntityType entityType,
                         String entityId,
                         AuditAction action,
                         String actorId,
                         Object payloadObj) {
        // Build the record outside any try-catch so that errors in enum formatting
        // or field assignment surface as plain RuntimeExceptions, not as mislabelled
        // serialisation or persistence failures.
        AuditLogRecord record = new AuditLogRecord();
        record.setLogId(UUID.randomUUID().toString());
        record.setEntityType(entityType.name());
        record.setEntityId(entityId);
        record.setAction(action.name());
        record.setActorId(actorId);
        record.setTimestamp(Instant.now());

        // Serialisation failure: caller supplied an un-serialisable payload object.
        try {
            record.setPayload(objectMapper.writeValueAsString(payloadObj));
        } catch (JsonProcessingException e) {
            throw new AuditLogException(
                    "Failed to serialise audit payload for action " + action, e);
        }

        // Persistence failure: DynamoDbException or other DAO-layer RuntimeException.
        // Wrapping ensures logAsync's catch block always sees AuditLogException,
        // preventing a raw DynamoDbException from silently killing the executor thread.
        try {
            auditLogDao.save(record);
        } catch (RuntimeException e) {
            throw new AuditLogException(
                    "Failed to persist audit log record for action " + action, e);
        }
    }

    // ── Checked exception ──────────────────────────────────────────────────────

    /**
     * Thrown by {@link #logSync} when the record cannot be written.
     * Not used by {@link #logAsync} (failures are swallowed there by design).
     */
    public static class AuditLogException extends RuntimeException {
        public AuditLogException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
