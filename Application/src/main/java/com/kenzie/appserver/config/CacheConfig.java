package com.kenzie.appserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration that registers the {@link CacheStore} bean.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Campaign cache with a 3-minute write expiry.
     *
     * @return the CacheStore bean
     */
    @Bean
    public CacheStore myCache() {
        return new CacheStore(180, TimeUnit.SECONDS);
    }
}
