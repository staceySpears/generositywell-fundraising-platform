package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CreateEventRequest;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.controller.model.EventUpdateRequest;
import com.kenzie.appserver.controller.model.RsvpRequest;
import com.kenzie.appserver.repositories.FundraisingEventDao;
import com.kenzie.appserver.repositories.model.FundraisingEventRecord;
import com.kenzie.appserver.service.model.AuditAction;
import com.kenzie.appserver.service.model.AuditEntityType;
import com.kenzie.appserver.service.model.EventStatus;
import com.kenzie.appserver.service.model.RsvpStatus;
import com.kenzie.appserver.service.model.Volunteer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for fundraising event lifecycle: creation, updates,
 * volunteer RSVP, waitlist promotion, and cancellation.
 *
 * <p>Cache key format: {@code "event:" + eventId}. This namespace avoids collisions
 * with the campaign entries stored by {@link CampaignService} (plain IDs).
 */
@Service
public class FundraisingEventService {

    private static final String CACHE_PREFIX = "event:";

    private final FundraisingEventDao eventDao;
    private final CacheStore<FundraisingEventRecord> cache;
    private final AuditLogService auditLogService;

    public FundraisingEventService(FundraisingEventDao eventDao,
                                   @Qualifier("eventCache") CacheStore<FundraisingEventRecord> cache,
                                   AuditLogService auditLogService) {
        this.eventDao = eventDao;
        this.cache = cache;
        this.auditLogService = auditLogService;
    }

    // ── Reads ──────────────────────────────────────────────────────────────────

    /**
     * Returns the event with the given ID.
     * Results are served from the in-memory cache when available.
     *
     * @param eventId the event ID
     * @return the event response
     * @throws ResponseStatusException 400 if ID is blank, 404 if not found
     */
    public EventResponse getEventById(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID cannot be empty");
        }
        String cacheKey = CACHE_PREFIX + eventId;
        Optional<FundraisingEventRecord> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached.map(this::recordToResponse)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        }
        Optional<FundraisingEventRecord> record = eventDao.findById(eventId);
        cache.add(cacheKey, record);
        return record.map(this::recordToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    /**
     * Returns all events. Performs a full DynamoDB table scan.
     *
     * @return list of all event responses
     */
    public List<EventResponse> getAllEvents() {
        return eventDao.findAll().stream()
                .map(this::recordToResponse)
                .toList();
    }

    /**
     * Returns all events for a given campaign.
     *
     * @param campaignId the campaign ID
     * @return list of matching event responses
     * @throws ResponseStatusException 400 if campaignId is blank
     */
    public List<EventResponse> getEventsByCampaign(String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        return eventDao.findByCampaignId(campaignId).stream()
                .map(this::recordToResponse)
                .toList();
    }

    // ── Writes ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new event in PLANNING status with an empty volunteer list.
     *
     * @param request the creation request
     * @return the created event response
     * @throws ResponseStatusException 400 if name or campaignId is blank,
     *         organizer is null, or registrationDeadline is after eventDate
     */
    public EventResponse createEvent(CreateEventRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event name is required");
        }
        if (request.getCampaignId() == null || request.getCampaignId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID is required");
        }
        if (request.getOrganizer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event must have an organizer");
        }
        if (request.getEventDate() != null && request.getRegistrationDeadline() != null
                && request.getRegistrationDeadline().isAfter(request.getEventDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration deadline must be on or before the event date");
        }

        FundraisingEventRecord record = new FundraisingEventRecord();
        record.setId(UUID.randomUUID().toString());
        record.setCampaignId(request.getCampaignId());
        record.setName(request.getName());
        record.setDescription(request.getDescription());
        record.setLocation(request.getLocation());
        record.setEventDate(request.getEventDate());
        record.setRegistrationDeadline(request.getRegistrationDeadline());
        record.setCapacity(request.getCapacity());
        record.setOrganizer(request.getOrganizer());
        record.setVolunteers(new ArrayList<>());
        record.setStatus(EventStatus.PLANNING.name());
        eventDao.save(record);

        auditLogService.logAsync(AuditEntityType.FUNDRAISING_EVENT, record.getId(),
                AuditAction.EVENT_CREATED, request.getOrganizer().getId(),
                Map.of("name", record.getName(), "campaignId", record.getCampaignId()));

        return recordToResponse(record);
    }

    /**
     * Updates an event's mutable fields.
     * Only the organizer (matched by JWT subject) may update.
     * COMPLETED and CANCELLED events cannot be modified.
     *
     * @param request          the update payload
     * @param requestingUserId the user ID from the authenticated JWT
     * @return the updated event response
     * @throws ResponseStatusException 404 if not found, 403 if not the organizer,
     *         409 if the event is already completed or cancelled
     */
    public EventResponse updateEvent(EventUpdateRequest request, String requestingUserId) {
        FundraisingEventRecord record = requireEvent(request.getId());
        requireOrganizer(record, requestingUserId);
        requireModifiable(record);

        if (request.getEventDate() != null && request.getRegistrationDeadline() != null
                && request.getRegistrationDeadline().isAfter(request.getEventDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration deadline must be on or before the event date");
        }

        record.setName(request.getName());
        record.setDescription(request.getDescription());
        record.setLocation(request.getLocation());
        record.setEventDate(request.getEventDate());
        record.setRegistrationDeadline(request.getRegistrationDeadline());
        record.setCapacity(request.getCapacity());
        eventDao.save(record);
        cache.evict(CACHE_PREFIX + record.getId());

        auditLogService.logAsync(AuditEntityType.FUNDRAISING_EVENT, record.getId(),
                AuditAction.EVENT_UPDATED, requestingUserId,
                Map.of("name", record.getName()));

        return recordToResponse(record);
    }

    /**
     * Moves an event from PLANNING to SCHEDULED, opening it for RSVPs.
     * Only the organizer may publish.
     *
     * @param eventId          the event to publish
     * @param requestingUserId the user ID from the authenticated JWT
     * @return the updated event response
     * @throws ResponseStatusException 404 if not found, 403 if not the organizer,
     *         409 if the event is not in PLANNING status
     */
    public EventResponse publishEvent(String eventId, String requestingUserId) {
        FundraisingEventRecord record = requireEvent(eventId);
        requireOrganizer(record, requestingUserId);

        if (!EventStatus.PLANNING.name().equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only events in PLANNING status can be published");
        }

        record.setStatus(EventStatus.SCHEDULED.name());
        eventDao.save(record);
        cache.evict(CACHE_PREFIX + eventId);

        auditLogService.logAsync(AuditEntityType.FUNDRAISING_EVENT, eventId,
                AuditAction.EVENT_PUBLISHED, requestingUserId,
                Map.of("status", EventStatus.SCHEDULED.name()));

        return recordToResponse(record);
    }

    /**
     * Cancels a SCHEDULED or IN_PROGRESS event.
     * Only the organizer may cancel.
     *
     * @param eventId          the event to cancel
     * @param requestingUserId the user ID from the authenticated JWT
     * @return the updated event response with CANCELLED status
     * @throws ResponseStatusException 404 if not found, 403 if not the organizer,
     *         409 if the event is already completed or cancelled
     */
    public EventResponse cancelEvent(String eventId, String requestingUserId) {
        FundraisingEventRecord record = requireEvent(eventId);
        requireOrganizer(record, requestingUserId);
        requireModifiable(record);

        record.setStatus(EventStatus.CANCELLED.name());
        eventDao.save(record);
        cache.evict(CACHE_PREFIX + eventId);

        auditLogService.logAsync(AuditEntityType.FUNDRAISING_EVENT, eventId,
                AuditAction.EVENT_CANCELLED, requestingUserId,
                Map.of("status", EventStatus.CANCELLED.name()));

        return recordToResponse(record);
    }

    /**
     * Permanently deletes an event and evicts it from the cache.
     * Only PLANNING-status events may be deleted; use cancel for scheduled events.
     *
     * @param eventId          the event to delete
     * @param requestingUserId the user ID from the authenticated JWT
     * @throws ResponseStatusException 400 if ID is blank, 404 if not found,
     *         403 if not the organizer, 409 if the event is not in PLANNING status
     */
    public void deleteEvent(String eventId, String requestingUserId) {
        if (eventId == null || eventId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID cannot be empty");
        }
        FundraisingEventRecord record = requireEvent(eventId);
        requireOrganizer(record, requestingUserId);

        if (!EventStatus.PLANNING.name().equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PLANNING events can be deleted — use cancel for scheduled events");
        }

        eventDao.deleteById(eventId);
        cache.evict(CACHE_PREFIX + eventId);

        auditLogService.logAsync(AuditEntityType.FUNDRAISING_EVENT, eventId,
                AuditAction.EVENT_DELETED, requestingUserId,
                Map.of("eventId", eventId));
    }

    // ── RSVP ──────────────────────────────────────────────────────────────────

    /**
     * Adds a volunteer RSVP to a SCHEDULED event.
     *
     * <ul>
     *   <li>If capacity is unlimited or confirmed count is below capacity → CONFIRMED</li>
     *   <li>Otherwise → WAITLISTED</li>
     *   <li>If the volunteer already has an active RSVP → 409 Conflict</li>
     * </ul>
     *
     * @param eventId the event to RSVP to
     * @param request the volunteer details
     * @return the updated event response
     * @throws ResponseStatusException 404 if event not found,
     *         409 if not SCHEDULED, past the deadline, or volunteer already registered
     */
    public EventResponse rsvp(String eventId, RsvpRequest request) {
        FundraisingEventRecord record = requireEvent(eventId);

        if (!EventStatus.SCHEDULED.name().equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "RSVPs are only accepted for SCHEDULED events");
        }
        if (record.getRegistrationDeadline() != null
                && LocalDate.now().isAfter(record.getRegistrationDeadline())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The registration deadline has passed");
        }

        List<Volunteer> volunteers = mutableVolunteers(record);

        // Reject duplicate active RSVPs
        boolean alreadyRegistered = volunteers.stream()
                .anyMatch(v -> v.getId().equals(request.getVolunteerId())
                        && !RsvpStatus.CANCELLED.name().equals(v.getRsvpStatus()));
        if (alreadyRegistered) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Volunteer is already registered for this event");
        }

        long confirmedCount = volunteers.stream()
                .filter(v -> RsvpStatus.CONFIRMED.name().equals(v.getRsvpStatus()))
                .count();

        String status = (record.getCapacity() == null || confirmedCount < record.getCapacity())
                ? RsvpStatus.CONFIRMED.name()
                : RsvpStatus.WAITLISTED.name();

        Volunteer volunteer = new Volunteer(
                request.getVolunteerId(),
                request.getVolunteerName(),
                request.getVolunteerEmail(),
                status);
        volunteers.add(volunteer);
        record.setVolunteers(volunteers);
        eventDao.save(record);
        cache.evict(CACHE_PREFIX + eventId);

        AuditAction rsvpAction = RsvpStatus.CONFIRMED.name().equals(status)
                ? AuditAction.RSVP_CONFIRMED
                : AuditAction.RSVP_WAITLISTED;
        auditLogService.logAsync(AuditEntityType.RSVP, eventId,
                rsvpAction, request.getVolunteerId(),
                Map.of("volunteerId", request.getVolunteerId(), "rsvpStatus", status));

        return recordToResponse(record);
    }

    /**
     * Cancels a volunteer's RSVP.
     * If a CONFIRMED volunteer cancels and there are WAITLISTED volunteers,
     * the first waitlisted volunteer is automatically promoted to CONFIRMED.
     * A volunteer can only cancel their own RSVP (JWT subject must match volunteerId).
     *
     * @param eventId          the event ID
     * @param volunteerId      the volunteer's user ID
     * @param requestingUserId the user ID from the authenticated JWT
     * @return the updated event response
     * @throws ResponseStatusException 404 if event not found or volunteer not found,
     *         403 if the JWT subject does not match the volunteerId
     */
    public EventResponse cancelRsvp(String eventId, String volunteerId, String requestingUserId) {
        if (!Objects.equals(requestingUserId, volunteerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only cancel your own RSVP");
        }

        FundraisingEventRecord record = requireEvent(eventId);
        List<Volunteer> volunteers = mutableVolunteers(record);

        Volunteer target = volunteers.stream()
                .filter(v -> v.getId().equals(volunteerId)
                        && !RsvpStatus.CANCELLED.name().equals(v.getRsvpStatus()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Active RSVP not found for this volunteer"));

        boolean wasConfirmed = RsvpStatus.CONFIRMED.name().equals(target.getRsvpStatus());
        target.setRsvpStatus(RsvpStatus.CANCELLED.name());

        // Promote first waitlisted volunteer when a confirmed spot opens
        Optional<Volunteer> promoted = Optional.empty();
        if (wasConfirmed) {
            promoted = volunteers.stream()
                    .filter(v -> RsvpStatus.WAITLISTED.name().equals(v.getRsvpStatus()))
                    .findFirst();
            promoted.ifPresent(v -> v.setRsvpStatus(RsvpStatus.CONFIRMED.name()));
        }

        record.setVolunteers(volunteers);
        eventDao.save(record);
        cache.evict(CACHE_PREFIX + eventId);

        auditLogService.logAsync(AuditEntityType.RSVP, eventId,
                AuditAction.RSVP_CANCELLED, volunteerId,
                Map.of("volunteerId", volunteerId));

        promoted.ifPresent(v ->
                auditLogService.logAsync(AuditEntityType.RSVP, eventId,
                        AuditAction.RSVP_PROMOTED_FROM_WAITLIST, v.getId(),
                        Map.of("volunteerId", v.getId())));

        return recordToResponse(record);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private FundraisingEventRecord requireEvent(String eventId) {
        return eventDao.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void requireOrganizer(FundraisingEventRecord record, String requestingUserId) {
        if (record.getOrganizer() == null
                || !Objects.equals(requestingUserId, record.getOrganizer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the event organizer can perform this action");
        }
    }

    private void requireModifiable(FundraisingEventRecord record) {
        String status = record.getStatus();
        if (EventStatus.COMPLETED.name().equals(status) || EventStatus.CANCELLED.name().equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot modify a COMPLETED or CANCELLED event");
        }
    }

    /** Returns a mutable copy of the volunteer list (never null). */
    private List<Volunteer> mutableVolunteers(FundraisingEventRecord record) {
        List<Volunteer> existing = record.getVolunteers();
        return existing == null ? new ArrayList<>() : new ArrayList<>(existing);
    }

    private EventResponse recordToResponse(FundraisingEventRecord record) {
        List<Volunteer> volunteers = record.getVolunteers() != null
                ? record.getVolunteers() : List.of();

        long confirmed = volunteers.stream()
                .filter(v -> RsvpStatus.CONFIRMED.name().equals(v.getRsvpStatus()))
                .count();
        long waitlisted = volunteers.stream()
                .filter(v -> RsvpStatus.WAITLISTED.name().equals(v.getRsvpStatus()))
                .count();

        EventResponse response = new EventResponse();
        response.setId(record.getId());
        response.setCampaignId(record.getCampaignId());
        response.setName(record.getName());
        response.setDescription(record.getDescription());
        response.setLocation(record.getLocation());
        response.setEventDate(record.getEventDate());
        response.setRegistrationDeadline(record.getRegistrationDeadline());
        response.setCapacity(record.getCapacity());
        response.setOrganizer(record.getOrganizer());
        response.setVolunteers(volunteers);
        response.setStatus(record.getStatus());
        response.setConfirmedCount((int) confirmed);
        response.setWaitlistedCount((int) waitlisted);

        if (record.getCapacity() != null) {
            int remaining = Math.max(0, record.getCapacity() - (int) confirmed);
            response.setSpotsRemaining(remaining);
        }

        return response;
    }
}
