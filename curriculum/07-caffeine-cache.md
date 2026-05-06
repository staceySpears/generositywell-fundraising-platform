# 07 — Caffeine Cache: Replacing Guava and Building a Sensible Cache Layer

## Why this exists

The capstone used Guava's `Cache` implementation (`com.google.common.cache.CacheBuilder`). Guava
is a general-purpose Java utility library — its cache was added as a convenience, not as a
primary product. Caffeine is a purpose-built, high-performance caching library that Guava's
author wrote specifically to replace Guava Cache. It has better hit rates (using the W-TinyLFU
eviction algorithm), better concurrency performance, and is the default cache provider for
Spring Boot's `@Cacheable` abstraction.

Spring Boot 3's `spring-boot-starter-cache` auto-configures Caffeine if it is on the classpath.
Using Guava Cache alongside a Spring Boot cache starter is inconsistent — you end up with two
different caching systems that do not share configuration or metrics.

Beyond the library choice, the old `CacheStore` had structural problems:
- Two separate cache instances (`cache` for EventRecord, `cacheLambda` for EventResponse)
- The Lambda cache was populated by a method called `addToCash` (a typo) and was never read
- An unused `EventRepository` field was injected into the constructor
- The `get` method returned `Optional<EventRecord>` but EventService never called it after
  the Lambda removal

---

## What caching actually does

When `EventService.getEventById` is called, it goes to DynamoDB. DynamoDB calls are fast
(single-digit milliseconds), but they are network calls. If the same event is requested
repeatedly — for example, a campaign page that 500 people are viewing at once — you do not
want 500 DynamoDB reads for the same record.

A cache stores the result of the first read in memory. Subsequent reads return from memory
without touching DynamoDB. The cache entry expires after a configured time (`expireAfterWrite`),
after which the next read goes back to DynamoDB and refreshes the cache.

The tradeoff: if an event is updated, the cached version is stale until it expires. This is why
the update and delete operations call `cache.evict(id)` — they explicitly remove the cached
entry so the next read fetches fresh data.

---

## The code

### `CacheStore.java` — current state (Guava, simplified)

```java
public class CacheStore {

    private final Cache<String, Optional<EventRecord>> cache;   // (1)

    public CacheStore(int expiry, TimeUnit timeUnit) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(expiry, timeUnit)             // (2)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())  // (3)
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
```

**(1)** The cache is typed to `Optional<EventRecord>`. Wrapping in `Optional` distinguishes
"not in cache yet" (null returned by `getIfPresent`) from "looked it up and it wasn't in DynamoDB"
(empty Optional). This prevents the case where you cache a missing record, evict it, and then
re-fetch to find it still missing — a subtle bug called "cache penetration."

**(2)** `expireAfterWrite` — entries expire this long after they were written, regardless of
whether they have been read. After expiry, the next `get` goes to DynamoDB.

**(3)** `concurrencyLevel` matches the number of CPU cores. Guava Cache is a segmented hash map;
this sets how many segments it uses. Caffeine does not need this hint — it handles concurrency
internally.

---

### What changes in the Caffeine migration

The API is nearly identical. The main differences:

```java
// Guava — what we have now
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

Cache<String, Optional<EventRecord>> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expiry, timeUnit)
        .concurrencyLevel(Runtime.getRuntime().availableProcessors())
        .build();
```

```java
// Caffeine — what we are moving to
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

Cache<String, Optional<EventRecord>> cache = Caffeine.newBuilder()
        .expireAfterWrite(expiry, timeUnit)
        .build();
```

The `concurrencyLevel` call is removed — Caffeine does not expose it. The `CacheBuilder` class
becomes `Caffeine`. The `Cache` interface type changes package but has the same methods.

---

### Where caching is used in `EventService`

```java
public void deleteEvent(String eventId) {
    // ...
    eventDao.deleteById(eventId);
    cache.evict(eventId);              // removes stale cached entry
}

public EventResponse updateEventById(EventUpdateRequest request) {
    // ...
    eventDao.save(record);
    cache.evict(record.getId());       // removes stale cached entry
    return recordToResponse(record);
}
```

`getEventById` does not currently populate the cache — it always goes to the DAO. Populating
the cache on reads is the next step. Once you add it, `getEventById` becomes:

```java
public EventResponse getEventById(String id) {
    Optional<EventRecord> cached = cache.get(id);
    if (cached != null) {
        return cached.map(this::recordToResponse).orElse(null);
    }
    Optional<EventRecord> record = eventDao.findById(id);
    cache.add(id, record);
    return record.map(this::recordToResponse).orElse(null);
}
```

---

## What to understand

1. What is `expireAfterWrite` and how does it differ from `expireAfterAccess`?
2. Why do we cache `Optional<EventRecord>` instead of just `EventRecord`?
3. Why do the update and delete operations call `cache.evict` but the create operation does not?
4. If two requests for the same event arrive at the same time and the cache is empty, both will
   hit DynamoDB. This is called a "cache stampede." How would you prevent it?

---

## Exercise

→ [Exercise 02 — Migrate CacheStore to Caffeine](exercises/02-migrate-cache-to-caffeine.md)

## Next

[08 — Bean Validation](08-bean-validation.md)
