package com.kenzie.appserver.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kenzie.appserver.controller.model.EventResponse;
import com.kenzie.appserver.repositories.model.EventRecord;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CacheStore {

    private final Cache<String, Optional<EventRecord>> cache;

    public CacheStore(int expiry, TimeUnit timeUnit) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(expiry, timeUnit)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
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
