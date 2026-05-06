package com.kenzie.appserver.service;

import com.kenzie.appserver.config.CacheStore;
import com.kenzie.appserver.controller.model.CampaignResponse;
import com.kenzie.appserver.controller.model.CampaignUpdateRequest;
import com.kenzie.appserver.controller.model.CreateCampaignRequest;
import com.kenzie.appserver.repositories.CampaignDao;
import com.kenzie.appserver.repositories.model.CampaignRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CampaignService {

    private final CampaignDao campaignDao;
    private final CacheStore cache;

    public CampaignService(CampaignDao campaignDao, CacheStore cache) {
        this.campaignDao = campaignDao;
        this.cache = cache;
    }

    public CampaignResponse getCampaignById(String id) {
        Optional<CampaignRecord> cached = cache.get(id);
        if (cached != null) {
            return cached.map(this::recordToResponse).orElse(null);
        }
        Optional<CampaignRecord> record = campaignDao.findById(id);
        cache.add(id, record);
        return record.map(this::recordToResponse).orElse(null);
    }

    public CampaignResponse updateCampaign(CampaignUpdateRequest request) {
        CampaignRecord record = campaignDao.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (!record.getUser().getId().equals(request.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the campaign creator can update this campaign");
        }

        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setUser(request.getUser());
        record.setSupporters(request.getSupporters());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        campaignDao.save(record);
        cache.evict(record.getId());

        return recordToResponse(record);
    }

    public CampaignResponse addNewCampaign(CreateCampaignRequest request) {
        CampaignRecord record = new CampaignRecord();
        record.setId(UUID.randomUUID().toString());
        record.setName(request.getName());
        record.setDate(request.getDate());
        record.setUser(request.getUser());
        record.setSupporters(request.getSupporters());
        record.setAddress(request.getAddress());
        record.setDescription(request.getDescription());
        campaignDao.save(record);

        return recordToResponse(record);
    }

    public void deleteCampaign(String campaignId) {
        if (campaignId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign ID cannot be empty");
        }
        if (!campaignDao.existsById(campaignId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found");
        }
        campaignDao.deleteById(campaignId);
        cache.evict(campaignId);
    }

    public List<CampaignResponse> getAllCampaigns() {
        return campaignDao.findAll().stream()
                .map(this::recordToResponse)
                .toList();
    }

    private CampaignResponse recordToResponse(CampaignRecord record) {
        CampaignResponse response = new CampaignResponse();
        response.setId(record.getId());
        response.setName(record.getName());
        response.setDate(record.getDate());
        response.setUser(record.getUser());
        response.setSupporters(record.getSupporters());
        response.setAddress(record.getAddress());
        response.setDescription(record.getDescription());
        return response;
    }
}
