package com.kenzie.appserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.controller.model.DonationRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class CampaignQueryUtility {

    public final CampaignControllerClient campaignControllerClient;

    private final MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    public CampaignQueryUtility(MockMvc mvc) {
        this.mvc = mvc;
        this.campaignControllerClient = new CampaignControllerClient();
    }

    public class CampaignControllerClient {

        public ResultActions getCampaignById(String id) throws Exception {
            return mvc.perform(get("/campaigns/{id}", id)
                    .accept(MediaType.APPLICATION_JSON));
        }

        public ResultActions getAllCampaigns() throws Exception {
            return mvc.perform(get("/campaigns/all")
                    .accept(MediaType.APPLICATION_JSON));
        }

        public ResultActions addCampaign(CreateCampaignRequest request, String token) throws Exception {
            return mvc.perform(post("/campaigns")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .content(mapper.writeValueAsString(request)));
        }

        public ResultActions addCampaignNoAuth(CreateCampaignRequest request) throws Exception {
            return mvc.perform(post("/campaigns")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request)));
        }

        public ResultActions updateCampaign(CampaignUpdateRequest request, String token) throws Exception {
            return mvc.perform(put("/campaigns/{id}", request.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .content(mapper.writeValueAsString(request)));
        }

        public ResultActions deleteCampaign(String campaignId, String token) throws Exception {
            return mvc.perform(delete("/campaigns/{id}", campaignId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        }

        public ResultActions donate(String campaignId, DonationRequest request) throws Exception {
            return mvc.perform(post("/campaigns/{id}/donate", campaignId)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request)));
        }

        public ResultActions closeCampaign(String campaignId, String token) throws Exception {
            return mvc.perform(post("/campaigns/{id}/close", campaignId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        }
    }
}
