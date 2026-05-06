package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CreateEventRequest;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.controller.model.EventUpdateRequest;
import com.kenzie.appserver.repositories.EventDao;
import com.kenzie.appserver.repositories.model.EventRecord;
import com.kenzie.appserver.service.model.Attendee;
import com.kenzie.appserver.service.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventDao eventDao;

    @Mock
    private CacheStore cache;

    @InjectMocks
    private EventService eventService;

    /** ------------------------------------------------------------------------
     *  EventService.getEventById
     *  ------------------------------------------------------------------------ **/

    @Test
    void getEventById_cacheHit_returnsResponseWithoutCallingDao() {
        EventRecord record = eventRecord("event-1");
        when(cache.get("event-1")).thenReturn(Optional.of(record));

        EventResponse response = eventService.getEventById("event-1");

        assertNotNull(response);
        assertEquals("event-1", response.getId());
        assertEquals(record.getName(), response.getName());
        verify(eventDao, never()).findById(any());
    }

    @Test
    void getEventById_cacheMiss_queriesDaoAndPopulatesCache() {
        EventRecord record = eventRecord("event-2");
        when(cache.get("event-2")).thenReturn(null);
        when(eventDao.findById("event-2")).thenReturn(Optional.of(record));

        EventResponse response = eventService.getEventById("event-2");

        assertNotNull(response);
        assertEquals("event-2", response.getId());
        verify(cache).add(eq("event-2"), eq(Optional.of(record)));
    }

    @Test
    void getEventById_notFound_returnsNull() {
        when(cache.get("ghost")).thenReturn(null);
        when(eventDao.findById("ghost")).thenReturn(Optional.empty());

        EventResponse response = eventService.getEventById("ghost");

        assertNull(response);
        verify(cache).add(eq("ghost"), eq(Optional.empty()));
    }

    /** ------------------------------------------------------------------------
     *  EventService.addNewEvent
     *  ------------------------------------------------------------------------ **/

    @Test
    void addNewEvent_validRequest_savesAndReturnsResponse() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");

        CreateEventRequest request = new CreateEventRequest();
        request.setName("Fundraiser Gala");
        request.setDate(LocalDate.now().toString());
        request.setUser(user);
        request.setListOfAttending(new ArrayList<>());
        request.setAddress("123 Main St");
        request.setDescription("Annual gala");

        ArgumentCaptor<EventRecord> captor = ArgumentCaptor.forClass(EventRecord.class);

        EventResponse response = eventService.addNewEvent(request);

        verify(eventDao).save(captor.capture());
        EventRecord saved = captor.getValue();

        assertNotNull(response);
        assertDoesNotThrow(() -> UUID.fromString(response.getId()), "ID should be a valid UUID");
        assertEquals("Fundraiser Gala", response.getName());
        assertEquals(saved.getId(), response.getId());
        assertEquals(saved.getName(), response.getName());
        assertEquals(saved.getDate(), response.getDate());
    }

    /** ------------------------------------------------------------------------
     *  EventService.updateEventById
     *  ------------------------------------------------------------------------ **/

    @Test
    void updateEventById_userMatches_savesUpdatedRecord() {
        User user = new User("user-1", "Stacey", "stacey@example.com");
        EventRecord existing = eventRecord("event-3");
        existing.setUser(user);

        when(eventDao.findById("event-3")).thenReturn(Optional.of(existing));
        ArgumentCaptor<EventRecord> captor = ArgumentCaptor.forClass(EventRecord.class);

        EventUpdateRequest request = new EventUpdateRequest();
        request.setId("event-3");
        request.setName("Updated Name");
        request.setDate(LocalDate.now().toString());
        request.setUser(user);
        request.setListOfAttending(new ArrayList<>());
        request.setAddress("456 New Ave");
        request.setDescription("Updated description");

        EventResponse response = eventService.updateEventById(request);

        verify(eventDao).save(captor.capture());
        EventRecord saved = captor.getValue();
        assertEquals("Updated Name", saved.getName());
        assertEquals("456 New Ave", saved.getAddress());
        assertEquals("Updated Name", response.getName());
        verify(cache).evict("event-3");
    }

    @Test
    void updateEventById_eventNotFound_throwsNotFound() {
        when(eventDao.findById("missing")).thenReturn(Optional.empty());

        EventUpdateRequest request = new EventUpdateRequest();
        request.setId("missing");
        request.setUser(new User("user-1", "Stacey", "stacey@example.com"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEventById(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(eventDao, never()).save(any());
    }

    @Test
    void updateEventById_userMismatch_throwsForbidden() {
        User owner = new User("user-1", "Stacey", "stacey@example.com");
        User other = new User("user-2", "Jordan", "jordan@example.com");

        EventRecord existing = eventRecord("event-4");
        existing.setUser(owner);
        when(eventDao.findById("event-4")).thenReturn(Optional.of(existing));

        EventUpdateRequest request = new EventUpdateRequest();
        request.setId("event-4");
        request.setUser(other);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEventById(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(eventDao, never()).save(any());
    }

    /** ------------------------------------------------------------------------
     *  EventService.deleteEvent
     *  ------------------------------------------------------------------------ **/

    @Test
    void deleteEvent_emptyId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.deleteEvent("")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(eventDao, never()).deleteById(any());
    }

    @Test
    void deleteEvent_eventDoesNotExist_throwsNotFound() {
        String id = UUID.randomUUID().toString();
        when(eventDao.existsById(id)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.deleteEvent(id)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(eventDao, never()).deleteById(any());
    }

    @Test
    void deleteEvent_eventExists_deletesAndEvictsCache() {
        String id = UUID.randomUUID().toString();
        when(eventDao.existsById(id)).thenReturn(true);

        eventService.deleteEvent(id);

        verify(eventDao).deleteById(id);
        verify(cache).evict(id);
    }

    /** ------------------------------------------------------------------------
     *  EventService.getAllEvents
     *  ------------------------------------------------------------------------ **/

    @Test
    void getAllEvents_returnsMappedResponseList() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");
        List<Attendee> attendees = List.of(
                new Attendee(UUID.randomUUID().toString(), "Alex", "alex@example.com")
        );

        EventRecord e1 = eventRecord("event-a");
        e1.setUser(user);
        e1.setListOfAttending(attendees);

        EventRecord e2 = eventRecord("event-b");
        e2.setUser(user);
        e2.setListOfAttending(attendees);

        when(eventDao.findAll()).thenReturn(List.of(e1, e2));

        List<EventResponse> responses = eventService.getAllEvents();

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.getId().equals("event-a")));
        assertTrue(responses.stream().anyMatch(r -> r.getId().equals("event-b")));
    }

    // Builds a minimal EventRecord with the given id
    private EventRecord eventRecord(String id) {
        EventRecord record = new EventRecord();
        record.setId(id);
        record.setName("Test Event " + id);
        record.setDate(LocalDate.now().toString());
        record.setAddress("123 Test St");
        record.setDescription("Test description");
        record.setListOfAttending(new ArrayList<>());
        return record;
    }
}
