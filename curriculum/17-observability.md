# 17 — Observability: Micrometer, Prometheus, and CloudWatch

> **Phase 5 — Not yet implemented.**

---

## Why this exists

A deployed application you cannot observe is a black box. When something goes wrong — slow
response times, elevated error rates, memory pressure — you need data to diagnose it. Observability
is the practice of making your application's internal state visible from the outside.

GenerosityWell has Micrometer wired in already (it is in `Application/build.gradle`). Micrometer
is a metrics facade — it captures measurements (request count, latency, cache hit rate) and
forwards them to one or more monitoring backends. We have configured Prometheus and CloudWatch
as backends.

---

## The pieces already in place

```groovy
// Application/build.gradle
implementation 'io.micrometer:micrometer-core:1.8.3'
implementation 'io.micrometer:micrometer-registry-prometheus:1.8.3'
implementation 'io.micrometer:micrometer-registry-cloudwatch2:1.8.3'
```

Spring Boot Actuator auto-configures Micrometer and exposes metrics at `/actuator/prometheus`
(Prometheus scrape endpoint) and forwards to CloudWatch automatically.

---

## The metrics that matter for this platform

| Metric | Why it matters |
|---|---|
| `http.server.requests` (latency, by endpoint) | Slow endpoints catch DynamoDB or Salesforce latency issues |
| `cache.gets` / `cache.hits` | Low hit rate means cache TTL is too short or eviction is too aggressive |
| `donation.created` (custom counter) | Track payment volume; alert on sudden drops |
| `donation.failed` (custom counter) | Alert if failure rate spikes |
| JVM memory (`jvm.memory.used`) | Catch memory leaks before the instance crashes |

Custom metrics are added with `MeterRegistry`:

```java
@Service
public class EventService {
    private final Counter eventsCreated;

    public EventService(EventDao eventDao, CacheStore cache, MeterRegistry registry) {
        this.eventsCreated = registry.counter("events.created");
        // ...
    }

    public EventResponse addNewEvent(CreateEventRequest request) {
        // ...
        eventsCreated.increment();
        return recordToResponse(record);
    }
}
```

---

## What to understand before you build this

1. What is the difference between a metric, a log, and a trace? (These are the three pillars
   of observability.)
2. Prometheus "scrapes" metrics by pulling from your `/actuator/prometheus` endpoint on a
   schedule. CloudWatch uses a "push" model. What are the tradeoffs?
3. What is a CloudWatch alarm, and what would you alert on for a payment processing failure?
4. The cache hit rate is 30% — most requests are going to DynamoDB. What would you investigate
   first?

---

## You have reached the end of the curriculum.

If you have worked through all 17 modules and completed the exercises, you can walk through
every file in this repository and explain — with confidence — why it is written the way it is,
what tradeoff it represents, and what you would change if the requirements shifted.

That is the goal. Go get the job.
