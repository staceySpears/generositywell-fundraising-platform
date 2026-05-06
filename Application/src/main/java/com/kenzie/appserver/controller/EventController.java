package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.CreateEventRequest;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.controller.model.EventUpdateRequest;
import com.kenzie.appserver.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable("id") String id) {
        EventResponse eventResponse = eventService.getEventById(id);
        if (eventResponse == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(eventResponse);
    }

    @PostMapping
    public ResponseEntity<EventResponse> addEvent(@Valid @RequestBody CreateEventRequest createEventRequest){
        EventResponse eventResponse = eventService.addNewEvent(createEventRequest);
        return ResponseEntity.created(URI.create("/events/" + eventResponse.getId())).body(eventResponse);
    }

    // What do we want to be available to be updated?
    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(@Valid @RequestBody EventUpdateRequest eventUpdateRequest) {

        EventResponse eventResponse = eventService.updateEventById(eventUpdateRequest);

        return ResponseEntity.ok(eventResponse);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity deleteEventById(@PathVariable("eventId") String eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }

    // Get All Events

    @GetMapping("/all")
    public ResponseEntity<List<EventResponse>> getAllEvents() {

        List<EventResponse> eventsResponses = eventService.getAllEvents();

        return ResponseEntity.ok(eventsResponses);
    }

}
