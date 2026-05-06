package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CreateEventRequest;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.controller.model.EventUpdateRequest;
import com.kenzie.appserver.repositories.EventDao;
import com.kenzie.appserver.repositories.model.EventRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private final EventDao eventDao;
    private final CacheStore cache;

    public EventService(EventDao eventDao, CacheStore cache) {
        this.eventDao = eventDao;
        this.cache = cache;
    }

    public EventResponse getEventById(String id) {
        Optional<EventRecord> cached = cache.get(id);
        if (cached != null) {
            return cached.map(this::recordToResponse).orElse(null);
        }
        Optional<EventRecord> record = eventDao.findById(id);
        cache.add(id, record);
        return record.map(this::recordToResponse).orElse(null);
    }

    public EventResponse updateEventById(EventUpdateRequest request) {
        EventRecord record = eventDao.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!record.getUser().getId().equals(request.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event creator can update this event");
        }

        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setUser(request.getUser());
        record.setListOfAttending(request.getListOfAttending());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        eventDao.save(record);
        cache.evict(record.getId());

        return recordToResponse(record);
    }

    public EventResponse addNewEvent(CreateEventRequest request) {
        EventRecord record = new EventRecord();
        record.setId(UUID.randomUUID().toString());
        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setUser(request.getUser());
        record.setListOfAttending(request.getListOfAttending());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        eventDao.save(record);

        return recordToResponse(record);
    }

    public void deleteEvent(String eventId) {
        if (eventId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID cannot be empty");
        }
        if (!eventDao.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        eventDao.deleteById(eventId);
        cache.evict(eventId);
    }

    public List<EventResponse> getAllEvents() {
        return eventDao.findAll().stream()
                .map(this::recordToResponse)
                .toList();
    }

    private EventResponse recordToResponse(EventRecord record) {
        EventResponse response = new EventResponse();
        response.setId(record.getId());
        response.setName(record.getName());
        response.setDate(record.getDate());
        response.setUser(record.getUser());
        response.setListOfAttending(record.getListOfAttending());
        response.setAddress(record.getAddress());
        response.setDescription(record.getDescription());
        return response;
    }
}
