package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.repositories.CampaignDao;
import com.kenzie.appserver.repositories.model.CampaignRecord;
import com.kenzie.appserver.salesforce.SalesforceService;
import com.kenzie.appserver.service.model.CampaignStatus;
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

    @Mock
    private SalesforceService salesforceService;

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

        assertNull(campaignService.getCampaignById("ghost"));
        verify(cache).add(eq("ghost"), eq(Optional.empty()));
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.addNewCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void addNewCampaign_validRequest_setsInitialStateAndSaves() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");

        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Send the Wildcats to State Finals");
        request.setDate(LocalDate.now().toString());
        request.setDeadline(LocalDate.now().plusDays(30).toString());
        request.setCategory("Sports");
        request.setUser(user);
        request.setSupporters(new ArrayList<>());
        request.setDescription("Help us get to state!");
        request.setGoalAmount(200000L); // $2,000.00

        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        CampaignResponse response = campaignService.addNewCampaign(request);

        verify(campaignDao).save(captor.capture());
        CampaignRecord saved = captor.getValue();

        assertDoesNotThrow(() -> UUID.fromString(response.getId()));
        assertEquals("Send the Wildcats to State Finals", response.getName());
        assertEquals(0L, saved.getCurrentAmount());
        assertEquals(CampaignStatus.ACTIVE.name(), saved.getStatus());
        assertEquals(200000L, saved.getGoalAmount());
        assertEquals(0, response.getPercentFunded());
    }

    @Test
    void addNewCampaign_blankName_throwsBadRequest() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");

        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("   ");
        request.setGoalAmount(100000L);
        request.setUser(user);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addNewCampaign(request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void addNewCampaign_nullGoalAmount_throwsBadRequest() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");

        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Valid Name");
        request.setGoalAmount(null);
        request.setUser(user);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addNewCampaign(request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void addNewCampaign_zeroGoalAmount_throwsBadRequest() {
        User user = new User(UUID.randomUUID().toString(), "Stacey", "stacey@example.com");

        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Valid Name");
        request.setGoalAmount(0L);
        request.setUser(user);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addNewCampaign(request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void addNewCampaign_nullUser_throwsBadRequest() {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Valid Name");
        request.setGoalAmount(100000L);
        request.setUser(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addNewCampaign(request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.addDonation
     *  ------------------------------------------------------------------------ **/

    @Test
    void addDonation_belowGoal_incrementsAmountStaysActive() {
        CampaignRecord record = campaignRecord("camp-3");
        record.setGoalAmount(200000L);
        record.setCurrentAmount(50000L);
        record.setStatus(CampaignStatus.ACTIVE.name());

        when(campaignDao.findById("camp-3")).thenReturn(Optional.of(record));
        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        CampaignResponse response = campaignService.addDonation("camp-3", 10000L);

        verify(campaignDao).save(captor.capture());
        assertEquals(60000L, captor.getValue().getCurrentAmount());
        assertEquals(CampaignStatus.ACTIVE.name(), captor.getValue().getStatus());
        assertEquals(30, response.getPercentFunded());
    }

    @Test
    void addDonation_hitsGoal_flipsStatusToFunded() {
        CampaignRecord record = campaignRecord("camp-4");
        record.setGoalAmount(200000L);
        record.setCurrentAmount(190000L);
        record.setStatus(CampaignStatus.ACTIVE.name());

        when(campaignDao.findById("camp-4")).thenReturn(Optional.of(record));
        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        campaignService.addDonation("camp-4", 10000L);

        verify(campaignDao).save(captor.capture());
        assertEquals(CampaignStatus.FUNDED.name(), captor.getValue().getStatus());
        assertEquals(200000L, captor.getValue().getCurrentAmount());
    }

    @Test
    void addDonation_exceedsGoal_overfundingAllowed() {
        CampaignRecord record = campaignRecord("camp-5");
        record.setGoalAmount(200000L);
        record.setCurrentAmount(195000L);
        record.setStatus(CampaignStatus.ACTIVE.name());

        when(campaignDao.findById("camp-5")).thenReturn(Optional.of(record));
        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        CampaignResponse response = campaignService.addDonation("camp-5", 20000L);

        verify(campaignDao).save(captor.capture());
        assertEquals(215000L, captor.getValue().getCurrentAmount());
        assertEquals(CampaignStatus.FUNDED.name(), captor.getValue().getStatus());
        assertEquals(107, response.getPercentFunded());
    }

    @Test
    void addDonation_alreadyFunded_staysAtFunded() {
        CampaignRecord record = campaignRecord("camp-6");
        record.setGoalAmount(200000L);
        record.setCurrentAmount(200000L);
        record.setStatus(CampaignStatus.FUNDED.name());

        when(campaignDao.findById("camp-6")).thenReturn(Optional.of(record));
        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        campaignService.addDonation("camp-6", 5000L);

        verify(campaignDao).save(captor.capture());
        assertEquals(CampaignStatus.FUNDED.name(), captor.getValue().getStatus());
        assertEquals(205000L, captor.getValue().getCurrentAmount());
    }

    @Test
    void addDonation_nullCampaignId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addDonation(null, 1000L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).findById(any());
    }

    @Test
    void addDonation_blankCampaignId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addDonation("  ", 1000L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).findById(any());
    }

    @Test
    void addDonation_nullAmount_throwsBadRequest() {
        CampaignRecord record = campaignRecord("camp-null-amt");
        when(campaignDao.findById("camp-null-amt")).thenReturn(Optional.of(record));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addDonation("camp-null-amt", null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void addDonation_zeroAmount_throwsBadRequest() {
        CampaignRecord record = campaignRecord("camp-zero-amt");
        when(campaignDao.findById("camp-zero-amt")).thenReturn(Optional.of(record));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addDonation("camp-zero-amt", 0L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void addDonation_negativeAmount_throwsBadRequest() {
        CampaignRecord record = campaignRecord("camp-neg-amt");
        when(campaignDao.findById("camp-neg-amt")).thenReturn(Optional.of(record));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addDonation("camp-neg-amt", -500L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    @Test
    void addDonation_closedCampaign_throwsConflict() {
        CampaignRecord record = campaignRecord("camp-7");
        record.setStatus(CampaignStatus.CLOSED.name());
        record.setCurrentAmount(0L);

        when(campaignDao.findById("camp-7")).thenReturn(Optional.of(record));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.addDonation("camp-7", 1000L)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(campaignDao, never()).save(any());
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.closeCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void closeCampaign_owner_setsStatusToClosed() {
        User owner = new User("user-1", "Stacey", "stacey@example.com");
        CampaignRecord record = campaignRecord("camp-8");
        record.setUser(owner);
        record.setStatus(CampaignStatus.ACTIVE.name());

        when(campaignDao.findById("camp-8")).thenReturn(Optional.of(record));
        ArgumentCaptor<CampaignRecord> captor = ArgumentCaptor.forClass(CampaignRecord.class);

        campaignService.closeCampaign("camp-8", "user-1");

        verify(campaignDao).save(captor.capture());
        assertEquals(CampaignStatus.CLOSED.name(), captor.getValue().getStatus());
        verify(cache).evict("camp-8");
    }

    @Test
    void closeCampaign_notOwner_throwsForbidden() {
        User owner = new User("user-1", "Stacey", "stacey@example.com");
        CampaignRecord record = campaignRecord("camp-9");
        record.setUser(owner);

        when(campaignDao.findById("camp-9")).thenReturn(Optional.of(record));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.closeCampaign("camp-9", "user-2")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.updateCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void updateCampaign_userMatches_savesUpdatedRecord() {
        User user = new User("user-1", "Stacey", "stacey@example.com");
        CampaignRecord existing = campaignRecord("camp-10");
        existing.setUser(user);

        when(campaignDao.findById("camp-10")).thenReturn(Optional.of(existing));

        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setId("camp-10");
        request.setName("Updated Name");
        request.setDate(LocalDate.now().toString());
        request.setDeadline(LocalDate.now().plusDays(60).toString());
        request.setCategory("Arts");
        request.setUser(user);
        request.setSupporters(new ArrayList<>());
        request.setDescription("Updated description");
        request.setGoalAmount(300000L);

        CampaignResponse response = campaignService.updateCampaign(request, "user-1");

        assertEquals("Updated Name", response.getName());
        verify(cache).evict("camp-10");
    }

    @Test
    void updateCampaign_notOwner_throwsForbidden() {
        User owner = new User("user-1", "Stacey", "stacey@example.com");
        CampaignRecord record = campaignRecord("camp-10b");
        record.setUser(owner);

        when(campaignDao.findById("camp-10b")).thenReturn(Optional.of(record));

        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setId("camp-10b");
        request.setName("Hijacked");
        request.setDate(LocalDate.now().toString());
        request.setDeadline(LocalDate.now().plusDays(30).toString());
        request.setCategory("Arts");
        request.setUser(owner);
        request.setSupporters(new ArrayList<>());
        request.setDescription("desc");
        request.setGoalAmount(100000L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.updateCampaign(request, "attacker-id")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateCampaign_closedCampaign_throwsConflict() {
        User user = new User("user-1", "Stacey", "stacey@example.com");
        CampaignRecord record = campaignRecord("camp-11");
        record.setUser(user);
        record.setStatus(CampaignStatus.CLOSED.name());

        when(campaignDao.findById("camp-11")).thenReturn(Optional.of(record));

        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setId("camp-11");
        request.setUser(user);
        request.setGoalAmount(100000L);
        request.setName("name");
        request.setDate(LocalDate.now().toString());
        request.setDeadline(LocalDate.now().plusDays(30).toString());
        request.setCategory("Sports");
        request.setDescription("desc");
        request.setSupporters(new ArrayList<>());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.updateCampaign(request, "user-1")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    /** ------------------------------------------------------------------------
     *  CampaignService.deleteCampaign
     *  ------------------------------------------------------------------------ **/

    @Test
    void deleteCampaign_nullId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.deleteCampaign(null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deleteCampaign_blankId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.deleteCampaign("   ")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deleteCampaign_emptyId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> campaignService.deleteCampaign("")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
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
    void getAllCampaigns_returnsMappedList() {
        CampaignRecord c1 = campaignRecord("camp-a");
        CampaignRecord c2 = campaignRecord("camp-b");
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
        record.setDeadline(LocalDate.now().plusDays(30).toString());
        record.setCategory("Community");
        record.setAddress("123 Test St");
        record.setDescription("Test description");
        record.setSupporters(new ArrayList<>());
        record.setGoalAmount(100000L);
        record.setCurrentAmount(0L);
        record.setStatus(CampaignStatus.ACTIVE.name());
        return record;
    }
}
