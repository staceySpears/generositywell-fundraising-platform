# Changelog — GenerosityWell

All significant changes to this project are documented here, organized by phase.
Changes within each phase are listed in reverse chronological order.

---

## Phase 2 — Domain Rename & Model Cleanup (In Progress · May 2026)

### Removed
- `ApplicationStartUpListener.java` — empty event listener, no active logic
- `Scheduler.java` — fully commented out, no active logic
- `BaseController.java` — bare `GET /` returning 200; Actuator handles health at `/actuator/health`
- `Example.java` — unused capstone domain stub
- `LambdaServiceClientConfiguration.java` — Spring `@Configuration` bean with no remaining consumers
- `ServiceLambdaModel` and `ServiceLambdaJavaClient` from `Application/build.gradle` dependencies

### Fixed
- `javax.validation.constraints` → `jakarta.validation.constraints` across all request/model DTOs
  (`CreateEventRequest`, `EventUpdateRequest`, `CreateUserRequest`, `UserUpdateRequest`, `Customer`)
- `.DS_Store` and `.claude/` added to `.gitignore`

---

## Phase 1 — Architecture Stabilization (Complete · March–May 2026)

### Added
- `EventDao` in `Application` — `DynamoDbEnhancedClient`-backed DAO; replaces broken `EventRepository`
- `UserDao` in `Application` — `DynamoDbEnhancedClient`-backed DAO; replaces broken `EventUserRepository`
- `GlobalExceptionHandler` with `@RestControllerAdvice` for standardized error responses across all endpoints
- `DynamoDbConfig` — `DynamoDbClient` and `DynamoDbEnhancedClient` Spring beans with local endpoint override support
- `id 'java'` plugin added to `Application/build.gradle` (required for Gradle 8.7 multi-project builds)

### Changed
- `EventService` — Lambda path removed; duplicate local/lambda methods consolidated into single `EventDao`-backed methods; status codes corrected (403 for auth failure, 404 for missing resource)
- `UserService` — `EventUserRepository` replaced with `UserDao`; `orElseThrow` pattern adopted
- `CacheStore` — dead `EventRepository` field and unused Lambda response cache removed
- `ata-curriculum.snippets-conventions.gradle` — `.enabled` → `.required` (Gradle 8 Report API change)
- `ServiceLambda/EventRecord.java` — stray closing brace removed; cross-module imports corrected to `com.kenzie.capstone.service.model`
- `EventDao` (ServiceLambda) — migrated from SDK v1 `DynamoDBMapper` to SDK v2 `DynamoDbEnhancedClient`
- `DaoModule` (ServiceLambda) — wired `DynamoDbEnhancedClient` via Dagger; dropped SDK v1 `DynamoDBMapper`
- `UserTypeConverter` and `CustomerTypeConverter` — migrated to SDK v2 `AttributeConverter<T>` (JSON serialization via Gson)
- `DynamoDbClientProvider` (ServiceLambda) — migrated to SDK v2 `DynamoDbClient`
- Spring Boot upgraded from `2.6.3` → `3.2.5`
- Java upgraded from `11` → `21`
- Gradle upgraded from `7.0.2` → `8.7`
- AWS SDK upgraded from v1 → v2 (`software.amazon.awssdk:bom:2.25.23`)
- Frontend migrated from Webpack 4 / vanilla JS → React 18 + Vite

### Removed
- `EventRepository` — broken `org.socialsignin.spring.data.dynamodb` dependency; library was never declared in the build
- `EventUserRepository` — same; replaced by `UserDao`
- `ExampleRecord` — dead capstone scaffold
- Lambda proxy path from `EventService` (`LambdaServiceClient`, `addNewEvent`, `getEventById`, `getAllEvents` Lambda variants)

---

## Origin — Academic Capstone (2022, Kenzie Academy)

The codebase was originally built as a team academic capstone at Kenzie Academy. It implemented
a basic event management system with the following architecture:

- Spring Boot 2.6.3 / Java 11 / Gradle 7.0.2
- AWS SDK v1 (`DynamoDBMapper`) for DynamoDB access
- Spring Data DynamoDB (`org.socialsignin`) for repository interfaces
- AWS Lambda for DynamoDB proxy (GetEventById, PostEvent, GetAllEvents)
- Caffeine in-memory cache at the application layer; Redis/Jedis at the Lambda layer
- Vanilla JS + Webpack 4 frontend

Known bugs at handoff:
- `EventService.java` — `lambdaResponse.toString() == ""` (reference equality, should be `.isEmpty()`)
- `EventService.java` — `getAllEvents()` called Lambda then ignored the result, returning local data only
- `EventService.java` — user validation commented out in `addNewEventLocally`
- `DonationController` — missing `@RestController` annotation
