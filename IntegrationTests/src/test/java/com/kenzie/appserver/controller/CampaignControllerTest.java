package com.kenzie.appserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kenzie.appserver.IntegrationTest;
import com.kenzie.appserver.controller.model.*;
import com.kenzie.appserver.security.JwtUtil;
import com.kenzie.appserver.service.CampaignService;
import com.kenzie.appserver.service.UserService;
import com.kenzie.appserver.service.model.User;
import net.andreinc.mockneat.MockNeat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private final MockNeat mockNeat = MockNeat.threadLocal();
    private final ObjectMapper mapper = new ObjectMapper();

    private CampaignQueryUtility campaignQueryUtility;

    private String ownerToken;
    private String ownerUserId;

    @BeforeAll
    void setup() {
        campaignQueryUtility = new CampaignQueryUtility(mvc);

        // Create a user directly via the service so @BeforeAll does not depend on HTTP
        CreateUserRequest req = new CreateUserRequest();
        req.setName("Campaign Owner");
        req.setEmail("owner-" + UUID.randomUUID() + "@test.com");
        req.setPassword("TestPass123!");
        UserResponse created = userService.createUser(req);

        ownerUserId = created.getId();
        ownerToken = jwtUtil.generateToken(ownerUserId, created.getEmail());
    }

    /** ------------------------------------------------------------------------
     *  GET /campaigns/{id}
     *  ------------------------------------------------------------------------ **/

    @Test
    void getCampaignById_validId_isSuccessful() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequest());

        campaignQueryUtility.campaignControllerClient.getCampaignById(created.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(is(created.getId())))
                .andExpect(jsonPath("$.name").value(is(created.getName())))
                .andExpect(jsonPath("$.status").value(is("ACTIVE")));
    }

    @Test
    void getCampaignById_notFound_returns404() throws Exception {
        campaignQueryUtility.campaignControllerClient.getCampaignById(UUID.randomUUID().toString())
                .andExpect(status().isNotFound());
    }

    /** ------------------------------------------------------------------------
     *  POST /campaigns
     *  ------------------------------------------------------------------------ **/

    @Test
    void addCampaign_validRequest_isSuccessful() throws Exception {
        CreateCampaignRequest request = buildCreateRequest();

        campaignQueryUtility.campaignControllerClient.addCampaign(request, ownerToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(is(request.getName())))
                .andExpect(jsonPath("$.status").value(is("ACTIVE")))
                .andExpect(jsonPath("$.currentAmount").value(is(0)))
                .andExpect(jsonPath("$.percentFunded").value(is(0)));
    }

    @Test
    void addCampaign_noAuth_returnsUnauthorized() throws Exception {
        campaignQueryUtility.campaignControllerClient.addCampaignNoAuth(buildCreateRequest())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCampaign_nullBody_returnsBadRequest() throws Exception {
        mvc.perform(post("/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    /** ------------------------------------------------------------------------
     *  PUT /campaigns/{id}
     *  ------------------------------------------------------------------------ **/

    @Test
    void updateCampaign_owner_isSuccessful() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequestOwnedBy(ownerUserId));

        CampaignUpdateRequest update = new CampaignUpdateRequest();
        update.setId(created.getId());
        update.setName("Updated: " + mockNeat.strings().get());
        update.setDate(LocalDate.now().toString());
        update.setDeadline(LocalDate.now().plusDays(60).toString());
        update.setCategory("Education");
        update.setUser(created.getUser());
        update.setSupporters(new ArrayList<>());
        update.setDescription("Updated description");
        update.setGoalAmount(300000L);

        campaignQueryUtility.campaignControllerClient.updateCampaign(update, ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(is(update.getName())))
                .andExpect(jsonPath("$.goalAmount").value(is(300000)));
    }

    @Test
    void updateCampaign_nonOwner_returnsForbidden() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequestOwnedBy(ownerUserId));

        String nonOwnerToken = jwtUtil.generateToken(UUID.randomUUID().toString(), "other@test.com");

        CampaignUpdateRequest update = new CampaignUpdateRequest();
        update.setId(created.getId());
        update.setName("Hijacked");
        update.setDate(LocalDate.now().toString());
        update.setDeadline(LocalDate.now().plusDays(60).toString());
        update.setCategory("Education");
        update.setUser(created.getUser());
        update.setSupporters(new ArrayList<>());
        update.setDescription("desc");
        update.setGoalAmount(100000L);

        campaignQueryUtility.campaignControllerClient.updateCampaign(update, nonOwnerToken)
                .andExpect(status().isForbidden());
    }

    /** ------------------------------------------------------------------------
     *  DELETE /campaigns/{id}
     *  ------------------------------------------------------------------------ **/

    @Test
    void deleteCampaign_validId_isSuccessful() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequest());

        campaignQueryUtility.campaignControllerClient.deleteCampaign(created.getId(), ownerToken)
                .andExpect(status().isOk());

        campaignQueryUtility.campaignControllerClient.getCampaignById(created.getId())
                .andExpect(status().isNotFound());
    }

    /** ------------------------------------------------------------------------
     *  GET /campaigns/all
     *  ------------------------------------------------------------------------ **/

    @Test
    void getAllCampaigns_isSuccessful() throws Exception {
        campaignService.addNewCampaign(buildCreateRequest());
        campaignService.addNewCampaign(buildCreateRequest());

        campaignQueryUtility.campaignControllerClient.getAllCampaigns()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    /** ------------------------------------------------------------------------
     *  POST /campaigns/{id}/donate
     *  ------------------------------------------------------------------------ **/

    @Test
    void donate_belowGoal_incrementsAmountStaysActive() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequest()); // goal = $100.00

        DonationRequest donation = new DonationRequest();
        donation.setAmount(5000L); // $50.00

        campaignQueryUtility.campaignControllerClient.donate(created.getId(), donation)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(is(5000)))
                .andExpect(jsonPath("$.status").value(is("ACTIVE")))
                .andExpect(jsonPath("$.percentFunded").value(is(50)));
    }

    @Test
    void donate_hitsGoal_flipsToFunded() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequest()); // goal = $100.00

        DonationRequest donation = new DonationRequest();
        donation.setAmount(10000L); // $100.00 — exact goal

        campaignQueryUtility.campaignControllerClient.donate(created.getId(), donation)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(is("FUNDED")))
                .andExpect(jsonPath("$.percentFunded").value(is(100)));
    }

    /** ------------------------------------------------------------------------
     *  POST /campaigns/{id}/close
     *  ------------------------------------------------------------------------ **/

    @Test
    void closeCampaign_owner_isSuccessful() throws Exception {
        CampaignResponse created = campaignService.addNewCampaign(buildCreateRequest());

        campaignQueryUtility.campaignControllerClient.closeCampaign(created.getId(), ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(is("CLOSED")));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CreateCampaignRequest buildCreateRequest() {
        return buildCreateRequestOwnedBy(ownerUserId);
    }

    private CreateCampaignRequest buildCreateRequestOwnedBy(String userId) {
        User user = new User(userId, mockNeat.names().get(), mockNeat.emails().get());

        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName(mockNeat.names().first().get() + " Campaign");
        request.setDate(LocalDate.now().toString());
        request.setDeadline(LocalDate.now().plusDays(30).toString());
        request.setCategory("Community");
        request.setUser(user);
        request.setSupporters(new ArrayList<>());
        request.setAddress("123 Test Street");
        request.setDescription("Help us reach our goal!");
        request.setGoalAmount(10000L); // $100.00
        return request;
    }
}
