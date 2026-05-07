package com.kenzie.appserver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kenzie.appserver.repositories.model.CampaignRecord;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * In-memory Caffeine cache for {@link CampaignRecord} lookups.
 * Caches both hits (present Optional) and misses (empty Optional) to prevent
 * repeated DynamoDB calls for non-existent IDs.
 */
public class CacheStore {

    private final Cache<String, Optional<CampaignRecord>> cache;

    public CacheStore(int expiry, TimeUnit timeUnit) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expiry, timeUnit)
                .build();
    }

    /**
     * Returns the cached value for the given key, or {@code null} if the key is not cached.
     * A return value of {@code Optional.empty()} means the key was cached as a miss.
     *
     * @param key the campaign ID
     * @return the cached Optional, or {@code null} if absent from the cache
     */
    public Optional<CampaignRecord> get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * Stores a value in the cache, including empty Optionals for miss caching.
     *
     * @param key   the campaign ID
     * @param value the record wrapped in Optional (empty for misses)
     */
    public void add(String key, Optional<CampaignRecord> value) {
        cache.put(key, value);
    }

    /**
     * Removes the entry for the given key, forcing the next read to go to DynamoDB.
     *
     * @param key the campaign ID to evict
     */
    public void evict(String key) {
        cache.invalidate(key);
    }
}
