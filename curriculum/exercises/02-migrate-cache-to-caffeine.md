# Exercise 02 — Migrate CacheStore from Guava to Caffeine

**Covers:** Module 07 — Caffeine Cache
**Difficulty:** Beginner
**Estimated time:** 20–30 minutes

---

## What you are doing

Swapping `CacheStore.java` from Guava Cache to Caffeine. The API is nearly identical, so the
change is small — but understanding *why* you are making it is the point.

---

## Before you start

Read `Application/src/main/java/com/kenzie/appserver/config/CacheStore.java` in full.
Note the imports and the `CacheBuilder` usage. Then read the Caffeine README entry in
`Application/build.gradle` — find the dependency and note the version.

Answer this question before touching the code: *why is the Caffeine dependency version
`2.9.3` in `build.gradle` potentially a problem for a Spring Boot 3 project?*

(Hint: Spring Boot 3 manages Caffeine via its BOM. Specifying your own version overrides
BOM management. Look up what version the Spring Boot 3.2.5 BOM pins for Caffeine.)

---

## Steps

**1.** In `Application/build.gradle`, change the Caffeine dependency:

```groovy
// Remove the explicit version — let Spring Boot's BOM manage it
implementation group: 'com.github.ben-manes.caffeine', name: 'caffeine'
```

**2.** In `CacheStore.java`, replace the Guava imports:

```java
// Remove
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

// Add
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
```

**3.** In the constructor, replace `CacheBuilder.newBuilder()` with `Caffeine.newBuilder()`.
Remove the `.concurrencyLevel(...)` call — Caffeine does not expose this setting.

**4.** Run `./gradlew :Application:compileJava`. Fix any errors.

**5.** Now wire the cache read into `EventService.getEventById`. The method currently always
goes to the DAO. Update it to check the cache first, populate it on a miss.

**6.** Run the compile again. Commit:

```
refactor: migrate CacheStore from Guava to Caffeine; wire cache read in getEventById
```

---

## Verify your work

- `grep -r "google.common.cache" Application/src` returns no results
- `EventService.getEventById` checks the cache before calling `eventDao.findById`
- `./gradlew :Application:compileJava` passes

---

## What you should be able to explain after this

- What does `expireAfterWrite` mean, and how is it different from `expireAfterAccess`?
- Why is letting the Spring Boot BOM manage the Caffeine version safer than pinning it yourself?
- After your change, if an event is updated via `updateEventById`, will `getEventById`
  immediately return the new data or the old cached data? Why?
