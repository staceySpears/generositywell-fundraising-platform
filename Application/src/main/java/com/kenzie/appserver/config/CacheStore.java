package com.kenzie.appserver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.repositories.model.EventRecord;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CacheStore {

    private final Cache<String, Optional<EventRecord>> cache;

    public CacheStore(int expiry, TimeUnit timeUnit) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expiry, timeUnit)
                .build();
    }

    public Optional<EventRecord> get(String key) {
        return cache.getIfPresent(key);
    }

    public void add(String key, Optional<EventRecord> value) {
        cache.put(key, value);
    }

    public void evict(String key) {
        cache.invalidate(key);
    }
}
