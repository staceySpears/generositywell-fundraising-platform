package com.kenzie.appserver.config;

import com.kenzie.appserver.repositories.model.CampaignRecord;
import com.kenzie.appserver.repositories.model.FundraisingEventRecord;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration that registers typed {@link CacheStore} beans.
 *
 * <p>Each bean is named so services can inject the correct type via {@code @Qualifier}:
 * <ul>
 *   <li>{@code "campaignCache"} — used by {@link com.kenzie.appserver.service.CampaignService}</li>
 *   <li>{@code "eventCache"} — used by {@link com.kenzie.appserver.service.FundraisingEventService}</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Campaign cache with a 3-minute write expiry.
     *
     * @return the CacheStore bean for campaigns
     */
    @Bean("campaignCache")
    public CacheStore<CampaignRecord> campaignCache() {
        return new CacheStore<>(180, TimeUnit.SECONDS);
    }

    /**
     * Fundraising event cache with a 3-minute write expiry.
     *
     * @return the CacheStore bean for fundraising events
     */
    @Bean("eventCache")
    public CacheStore<FundraisingEventRecord> eventCache() {
        return new CacheStore<>(180, TimeUnit.SECONDS);
    }
}
