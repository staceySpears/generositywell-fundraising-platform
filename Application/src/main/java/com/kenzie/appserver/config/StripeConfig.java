package com.kenzie.appserver.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
/** Initialises the Stripe SDK with the API key from application properties. */
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    /**
     * Sets the global Stripe API key on application startup.
     * Must run before any Stripe API call is made.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
