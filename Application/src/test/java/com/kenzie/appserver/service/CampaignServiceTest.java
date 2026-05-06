package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.repositories.CampaignDao;
import com.kenzie.appserver.repositories.model.CampaignRecord;
import com.kenzie.appserver.service.model.Supporter;
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
class CampaignServiceTest {

    @Mock
    private CampaignDao campaignDao;

    @Mock
    private CacheStore cache;

    @InjectMocks
    private CampaignService campaignService;

    /** ------------------------------------------------------------------------
     *  CampaignService.getCampaignById
     *  ------------------------------------------------------------------------ **/

    @Test
    void getCampaignById_cacheHit_returnsResponseWithoutCallingDao() {
        CampaignRecord record = campaignRecord("camp-1");
        when(cache.get("camp-1")).thenReturn(Optional.of(record));

        CampaignResponse response = campaignService.getCampaignById("camp-1");

        assertNotNull(response);
        assertEquals("camp-1", response.getId());
        verify(campaignDao, never()).findById(any());
    }

    @Test
    void getCampaignById_cacheMiss_queriesDaoAndPopulatesCache() {
        CampaignRecord record = campaignRecord("camp-2");
        when(cache.get("camp-2")).thenReturn(null);
        when(campaignDao.findById("camp-2")).thenReturn(Optional.of(record));

        CampaignResponse response = campaignService.getCampaignById("camp-2");

        assertNotNull(response);
        verify(cache).add(eq("camp-2"), eq(Optional.of(record)));
    }

    @Test
    void getCampaignById_notFound_returnsNull() {
        when(cache.get("ghost")).thenReturn(null);
        when(campaignDao.findById("ghost")).thenReturn(Optional.empty());

        CampaignResponse response = campaignService.getCampaignById("ghost");

        assertNull(response);
        verify(cache).add(eq("ghost"), eq(Optional.empty()));
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.addNewCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void addNewCampaign_validRequest_savesAndReturnsResponse() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");

        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Send the Wildcats to State Finals");
        request.setDate(LocalDate.now().toString());
        request.setUser(user);
        request.setSupporters(new ArrayList<>());
        request.setAddress("123 Main St");
        request.setDescription("Help us get to state!");

        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        CampaignResponse response = campaignService.addNewCampaign(request);

        verify(campaignDao).save(captor.capture());
        CampaignRecord saved = captor.getValue();

        assertNotNull(response);
        assertDoesNotThrow(() -> UUID.fromString(response.getId()), "ID should be a valid UUID");
        assertEquals("Send the Wildcats to State Finals", response.getName());
        assertEquals(saved.getId(), response.getId());
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.updateCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void updateCampaign_userMatches_savesUpdatedRecord() {
        User user = new User("user-1", "Stacey", "stacey@example.com");
        CampaignRecord existing = campaignRecord("camp-3");
        existing.setUser(user);

        when(campaignDao.findById("camp-3")).thenReturn(Optional.of(existing));
        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setId("camp-3");
        request.setName("Updated Campaign Name");
        request.setDate(LocalDate.now().toString());
        request.setUser(user);
        request.setSupporters(new ArrayList<>());
        request.setAddress("456 New Ave");
        request.setDescription("Updated description");

        CampaignResponse response = campaignService.updateCampaign(request);

        verify(campaignDao).save(captor.capture());
        assertEquals("Updated Campaign Name", captor.getValue().getName());
        assertEquals("Updated Campaign Name", response.getName());
        verify(cache).evict("camp-3");
    }

    @Test
    void updateCampaign_notFound_throwsNotFound() {
        when(campaignDao.findById("missing")).thenReturn(Optional.empty());

        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setId("missing");
        request.setUser(new User("user-1", "Stacey", "stacey@example.com"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.updateCampaign(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void updateCampaign_userMismatch_throwsForbidden() {
        User owner = new User("user-1", "Stacey", "stacey@example.com");
        User other = new User("user-2", "Jordan", "jordan@example.com");

        CampaignRecord existing = campaignRecord("camp-4");
        existing.setUser(owner);
        when(campaignDao.findById("camp-4")).thenReturn(Optional.of(existing));

        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setId("camp-4");
        request.setUser(other);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.updateCampaign(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.deleteCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void deleteCampaign_emptyId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.deleteCampaign("")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).deleteById(any());
    }

    @Test
    void deleteCampaign_notFound_throwsNotFound() {
        String id = UUID.randomUUID().toString();
        when(campaignDao.existsById(id)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.deleteCampaign(id)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(campaignDao, never()).deleteById(any());
    }

    @Test
    void deleteCampaign_found_deletesAndEvictsCache() {
        String id = UUID.randomUUID().toString();
        when(campaignDao.existsById(id)).thenReturn(true);

        campaignService.deleteCampaign(id);

        verify(campaignDao).deleteById(id);
        verify(cache).evict(id);
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.getAllCampaigns
     *  ------------------------------------------------------------------------ **/

    @Test
    void getAllCampaigns_returnsMappedResponseList() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");
        List<Supporter> supporters = List.of(
                new Supporter(UUID.randomUUID().toString(), "Alex", "alex@example.com")
        );

        CampaignRecord c1 = campaignRecord("camp-a");
        c1.setUser(user);
        c1.setSupporters(supporters);

        CampaignRecord c2 = campaignRecord("camp-b");
        c2.setUser(user);
        c2.setSupporters(supporters);

        when(campaignDao.findAll()).thenReturn(List.of(c1, c2));

        List<CampaignResponse> responses = campaignService.getAllCampaigns();

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.getId().equals("camp-a")));
        assertTrue(responses.stream().anyMatch(r -> r.getId().equals("camp-b")));
    }

    private CampaignRecord campaignRecord(String id) {
        CampaignRecord record = new CampaignRecord();
        record.setId(id);
        record.setName("Test Campaign " + id);
        record.setDate(LocalDate.now().toString());
        record.setAddress("123 Test St");
        record.setDescription("Test description");
        record.setSupporters(new ArrayList<>());
        return record;
    }
}
