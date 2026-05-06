# 04 — Removing the Lambda Proxy: When Serverless is the Wrong Tool

## Why this exists

The capstone architecture routed all DynamoDB access through an AWS Lambda function:

```
Spring Boot → LambdaServiceClient → HTTP → API Gateway → Lambda → DynamoDB
```

This was an academic pattern designed to give students practice with Lambda. In production, it
is an antipattern for synchronous data access. Every read and write added a full HTTP round-trip,
a Lambda cold-start risk, and two separate sets of domain models to keep in sync. The Lambda was
not doing any business logic — it was a pure DynamoDB proxy.

The refactor replaced that entire path with direct Enhanced Client access:

```
Spring Boot → DynamoDbEnhancedClient → DynamoDB
```

This is not "Lambda is bad." Lambda is excellent for what it is designed for: event-driven,
asynchronous, stateless compute. We will bring Lambda back in Phase 4 — deliberately, for Stripe
webhook processing, where async event-driven architecture is exactly the right model. The lesson
is not "avoid Lambda." It is "use the right tool for the access pattern."

---

## What was wrong with the old code

### `EventService.java` — before

The old service had *two versions of every operation*: a Lambda path and a local path. Neither was
fully correct.

```java
// Lambda path — called the Lambda, returned its result
public EventResponse getEventById(String id) {
    EventResponseData lambdaResponse = lambdaServiceClient.getEventById(id);
    return lambdaDataToResponse(lambdaResponse);
}

// Local path — used the Spring Data repository directly
public EventResponse getEventByIdToLocal(String id) {
    Optional<EventRecord> record = eventRepository.findById(id);
    return record.map(this::recordToResponse).orElse(null);
}
```

`getAllEvents` was the worst example. It called the Lambda, then ignored the result:

```java
public List<EventResponse> getAllEvents() {
    List<EventResponseData> allOfEvents = this.lambdaServiceClient.getAllEvents(); // (1)
    Iterable<EventRecord> responseOfRecords = eventRepository.findAll();           // (2)
    // (1) is never used — the method returns (2)
}
```

The Lambda call on line (1) was dead code. Every `getAllEvents` request paid the Lambda round-trip
cost and then threw away the response.

---

### `EventRepository` and `EventUserRepository` — never functional

These were Spring Data interfaces that declared `CrudRepository` using the `org.socialsignin.spring.data.dynamodb`
library — a community-maintained adapter that was never declared as a dependency in `build.gradle`.
The classes compiled (the IDE cached resolved types) but the build would always fail cold. They
were replaced by `EventDao` and `UserDao` backed directly by `DynamoDbEnhancedClient`.

---

## The code after

### `EventService.java` — after

```java
@Service
public class EventService {

    private final EventDao eventDao;
    private final CacheStore cache;

    public EventService(EventDao eventDao, CacheStore cache) {
        this.eventDao = eventDao;
        this.cache = cache;
    }

    public EventResponse getEventById(String id) {
        return eventDao.findById(id)           // (1)
                .map(this::recordToResponse)   // (2)
                .orElse(null);                 // (3)
    }

    public List<EventResponse> getAllEvents() {
        return eventDao.findAll().stream()
                .map(this::recordToResponse)
                .toList();
    }
}
```

**(1)** `findById` returns `Optional<EventRecord>`. The DAO never throws; it returns empty if
the record does not exist.

**(2)** `map` transforms the record to a response only if it is present. This replaces the old
null check + manual conversion.

**(3)** `orElse(null)` returns null if the record was not found. The controller then calls
`ResponseEntity.notFound().build()`. The service does not throw on a missing record — that is
the controller's concern.

---

### Why Lambda comes back in Phase 4

Stripe sends payment webhook events to a URL you expose. Processing a webhook has specific
requirements that make Lambda the right choice:

- **Async** — Stripe does not wait for you to process the payment before the user sees a
  confirmation. You acknowledge receipt immediately and process asynchronously.
- **Stateless** — each webhook event is independent. Lambda's stateless execution model is
  exactly right.
- **Event-driven** — a payment event triggers processing. Nothing polls for new payments;
  Stripe pushes them.
- **Isolated failure domain** — a bug in payment processing should not affect event browsing
  or user login. Putting it in a separate Lambda enforces that isolation.

None of those properties applied to the original Lambda proxy. All of them apply to the Stripe
webhook handler. That is why the same technology choice is correct in one context and wrong
in another.

---

## What to understand

1. What does `Optional.map()` do, and why is it preferred over a null check followed by a
   manual call?
2. The old `EventService` had `lambdaDataToResponse()`, `recordToResponse()`, and
   `eventToResponse()` — three converter methods doing essentially the same job. Why did the
   refactor reduce this to one?
3. What is Lambda cold start, and why does it matter for synchronous API calls but not for
   async webhook processing?
4. The old `getAllEvents` called the Lambda and ignored the result. How would you have caught
   this bug with a unit test?

---

## Next

[05 — Global Exception Handling](05-exception-handling.md)
