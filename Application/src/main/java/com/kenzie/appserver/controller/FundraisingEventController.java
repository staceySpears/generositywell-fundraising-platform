package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.CreateEventRequest;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.controller.model.EventUpdateRequest;
import com.kenzie.appserver.controller.model.RsvpRequest;
import com.kenzie.appserver.service.FundraisingEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for fundraising event CRUD and volunteer RSVP.
 *
 * <p>Security model:
 * <ul>
 *   <li>GET endpoints are public (read-only event discovery)</li>
 *   <li>POST /events and PUT /events require a valid JWT</li>
 *   <li>POST /events/{id}/publish and /cancel require JWT + organizer ownership</li>
 *   <li>POST /events/{id}/rsvp requires a valid JWT (volunteer identity from JWT)</li>
 *   <li>DELETE /events/{id}/rsvp requires JWT matching the volunteerId path param</li>
 * </ul>
 */
@RestController
@RequestMapping("/events")
public class FundraisingEventController {

    private final FundraisingEventService eventService;

    FundraisingEventController(FundraisingEventService eventService) {
        this.eventService = eventService;
    }

    /**
     * {@code GET /events/{id}} — returns a single event by ID.
     * Public endpoint.
     *
     * @param id the event ID
     * @return 200 with the event, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable("id") String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * {@code GET /events/all} — returns all events.
     * Public endpoint. Full table scan — avoid high-frequency polling.
     *
     * @return 200 with the list of all events
     */
    @GetMapping("/all")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * {@code GET /events/campaign/{campaignId}} — returns all events for a campaign.
     * Public endpoint.
     *
     * @param campaignId the campaign ID
     * @return 200 with matching events
     */
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<EventResponse>> getEventsByCampaign(
            @PathVariable("campaignId") String campaignId) {
        return ResponseEntity.ok(eventService.getEventsByCampaign(campaignId));
    }

    /**
     * {@code POST /events} — creates a new event in PLANNING status.
     * Requires a valid JWT. Returns 201 with a Location header.
     *
     * @param request the event creation payload
     * @return 201 with the created event
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.created(URI.create("/events/" + response.getId())).body(response);
    }

    /**
     * {@code PUT /events/{id}} — updates an event's mutable fields.
     * Requires a valid JWT; caller must be the organizer.
     *
     * @param request        the update payload (must include the event ID)
     * @param authentication the authenticated principal
     * @return 200 with the updated event
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable("id") String id,
            @Valid @RequestBody EventUpdateRequest request,
            Authentication authentication) {
        request.setId(id);
        EventResponse response = eventService.updateEvent(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * {@code POST /events/{id}/publish} — moves an event from PLANNING to SCHEDULED.
     * Requires a valid JWT; caller must be the organizer.
     *
     * @param id             the event ID to publish
     * @param authentication the authenticated principal
     * @return 200 with the updated event
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable("id") String id,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.publishEvent(id, authentication.getName()));
    }

    /**
     * {@code POST /events/{id}/cancel} — cancels a SCHEDULED or IN_PROGRESS event.
     * Requires a valid JWT; caller must be the organizer.
     *
     * @param id             the event ID to cancel
     * @param authentication the authenticated principal
     * @return 200 with the cancelled event
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable("id") String id,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.cancelEvent(id, authentication.getName()));
    }

    /**
     * {@code DELETE /events/{id}} — permanently deletes a PLANNING-status event.
     * Requires a valid JWT; caller must be the organizer.
     *
     * @param id             the event ID to delete
     * @param authentication the authenticated principal
     * @return 204 on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable("id") String id,
            Authentication authentication) {
        eventService.deleteEvent(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code POST /events/{id}/rsvp} — submits a volunteer RSVP.
     * The volunteer is CONFIRMED if spots remain; otherwise WAITLISTED.
     * Requires a valid JWT.
     *
     * @param id             the event ID to RSVP to
     * @param request        the volunteer details
     * @param authentication the authenticated principal (used for JWT ownership verification
     *                       in {@link com.kenzie.appserver.service.FundraisingEventService})
     * @return 200 with the updated event
     */
    @PostMapping("/{id}/rsvp")
    public ResponseEntity<EventResponse> rsvp(
            @PathVariable("id") String id,
            @Valid @RequestBody RsvpRequest request,
            Authentication authentication) {
        // Override the volunteerId from JWT subject — the server, not the client,
        // determines who is RSVPing. Name and email are still accepted from the body.
        request.setVolunteerId(authentication.getName());
        return ResponseEntity.ok(eventService.rsvp(id, request));
    }

    /**
     * {@code DELETE /events/{id}/rsvp/{volunteerId}} — cancels a volunteer's RSVP.
     * The first waitlisted volunteer is automatically promoted when a confirmed spot opens.
     * Requires a valid JWT; the JWT subject must match {@code volunteerId}.
     *
     * @param id             the event ID
     * @param volunteerId    the volunteer's user ID
     * @param authentication the authenticated principal
     * @return 200 with the updated event
     */
    @DeleteMapping("/{id}/rsvp/{volunteerId}")
    public ResponseEntity<EventResponse> cancelRsvp(
            @PathVariable("id") String id,
            @PathVariable("volunteerId") String volunteerId,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.cancelRsvp(id, volunteerId, authentication.getName()));
    }
}
