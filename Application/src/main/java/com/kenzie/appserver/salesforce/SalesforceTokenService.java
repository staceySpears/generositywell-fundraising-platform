package com.kenzie.appserver.salesforce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Manages a cached Salesforce OAuth2 access token using the client credentials flow.
 * The token is refreshed proactively 5 minutes before expiry to avoid mid-request failures.
 */
@Component
public class SalesforceTokenService {

    private static final Logger log = LoggerFactory.getLogger(SalesforceTokenService.class);

    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final RestClient restClient;

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public SalesforceTokenService(
            @Value("${salesforce.client-id}") String clientId,
            @Value("${salesforce.client-secret}") String clientSecret,
            @Value("${salesforce.token-url}") String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.restClient = RestClient.create();
    }

    /**
     * Returns a valid Salesforce access token, refreshing it if it is expired or within
     * 5 minutes of expiry. Thread-safe via {@code synchronized}.
     *
     * @return a bearer token string ready for use in an Authorization header
     */
    public synchronized String getAccessToken() {
        if (cachedToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(300))) {
            refresh();
        }
        return cachedToken;
    }

    @SuppressWarnings("unchecked")
    private void refresh() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> response = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("Salesforce token response missing access_token");
        }

        cachedToken = (String) response.get("access_token");
        // SF client credentials tokens are valid for 2 hours; treat as 1 hour to be safe
        tokenExpiry = Instant.now().plusSeconds(3600);
        log.debug("Salesforce token refreshed");
    }
}
