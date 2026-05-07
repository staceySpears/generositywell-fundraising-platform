package com.kenzie.appserver.salesforce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Low-level HTTP client for the Salesforce REST API. Handles token injection and error mapping. */
@Component
public class SalesforceClient {

    private static final Logger log = LoggerFactory.getLogger(SalesforceClient.class);

    private final SalesforceTokenService tokenService;
    private final String instanceUrl;
    private final String apiVersion;
    private final RestClient restClient;

    public SalesforceClient(
            SalesforceTokenService tokenService,
            @Value("${salesforce.instance-url}") String instanceUrl,
            @Value("${salesforce.api-version}") String apiVersion) {
        this.tokenService = tokenService;
        this.instanceUrl = instanceUrl;
        this.apiVersion = apiVersion;
        this.restClient = RestClient.create();
    }

    /**
     * Creates a new sObject record in Salesforce and returns its ID.
     * Throws {@link RuntimeException} if the API response indicates failure or returns null.
     *
     * @param sobjectType the Salesforce sObject API name (e.g. "Campaign", "Opportunity")
     * @param fields      the field name-to-value map for the new record
     * @return the Salesforce ID of the created record
     */
    @SuppressWarnings("unchecked")
    public String create(String sobjectType, Map<String, Object> fields) {
        String url = instanceUrl + "/services/data/" + apiVersion + "/sobjects/" + sobjectType;

        Map<String, Object> response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(fields)
                .retrieve()
                .body(Map.class);

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            log.error("Salesforce create failed for {}: {}", sobjectType, response);
            throw new RuntimeException("Salesforce create failed for " + sobjectType);
        }

        return (String) response.get("id");
    }

    /**
     * Updates an existing sObject record in Salesforce by its ID.
     *
     * @param sobjectType the Salesforce sObject API name
     * @param sfId        the Salesforce record ID to update
     * @param fields      the fields to patch
     */
    public void update(String sobjectType, String sfId, Map<String, Object> fields) {
        String url = instanceUrl + "/services/data/" + apiVersion + "/sobjects/" + sobjectType + "/" + sfId;

        restClient.patch()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(fields)
                .retrieve()
                .toBodilessEntity();
    }
}
