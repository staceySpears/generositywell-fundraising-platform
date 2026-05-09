package com.kenzie.appserver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Generic in-memory Caffeine cache for DynamoDB record lookups.
 * Caches both hits (present Optional) and misses (empty Optional) to prevent
 * repeated DynamoDB calls for non-existent IDs.
 *
 * <p>Typed beans are registered in {@link CacheConfig}:
 * <ul>
 *   <li>{@code "campaignCache"} → {@code CacheStore<CampaignRecord>}</li>
 *   <li>{@code "eventCache"} → {@code CacheStore<FundraisingEventRecord>}</li>
 * </ul>
 *
 * @param <T> the DynamoDB record type stored in this cache
 */
public class CacheStore<T> {

    private final Cache<String, Optional<T>> cache;

    public CacheStore(int expiry, TimeUnit timeUnit) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expiry, timeUnit)
                .build();
    }

    /**
     * Returns the cached value for the given key, or {@code null} if the key is not cached.
     * A return value of {@code Optional.empty()} means the key was cached as a miss.
     *
     * @param key the record ID (may include a namespace prefix, e.g. {@code "event:uuid"})
     * @return the cached Optional, or {@code null} if absent from the cache
     */
    public Optional<T> get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * Stores a value in the cache, including empty Optionals for miss caching.
     *
     * @param key   the record ID
     * @param value the record wrapped in Optional (empty for misses)
     */
    public void add(String key, Optional<T> value) {
        cache.put(key, value);
    }

    /**
     * Removes the entry for the given key, forcing the next read to go to DynamoDB.
     *
     * @param key the record ID to evict
     */
    public void evict(String key) {
        cache.invalidate(key);
    }
}
