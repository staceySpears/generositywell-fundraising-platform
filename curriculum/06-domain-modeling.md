# 06 — Domain Modeling and Naming: Renaming the Capstone Entities

## Why this exists

The capstone used generic names inherited from an academic template: `Event`, `User`, `Customer`.
These names tell you nothing about the platform's purpose. A fundraising platform that calls
its contributors "Customers" immediately signals that no one thought about what the domain
actually represents.

Domain modeling is the practice of naming and structuring your code around the *business concepts*
your software represents — not around generic CRUD patterns or technical roles. When someone
reads your code, they should immediately understand what the software does without reading
documentation.

This module covers the renaming decisions and the structural changes they require.

---

## The naming decisions

### `Customer` → `Attendee`

`Customer` is an e-commerce term. GenerosityWell is not a store. Someone who shows up to a
fundraising event is an **Attendee**. This name also maps cleanly to the volunteer model:
an attendee can be a financial donor, a volunteer, or both. The word "Customer" cannot carry
that meaning without confusion.

### `User` stays `User` for now — but splits later

`User` is the platform account — the entity that logs in. But in this domain, a user has a *role*:
they are either an `Organizer` (creates and manages campaigns and events) or a `Donor` (gives
money or time). These will become explicit in Phase 3 when we add JWT auth and role-based access.
For now, the `User` class stays but we document the intent.

### A new entity: `Campaign`

The capstone had `Event` as the top-level concept. A fundraising platform needs a `Campaign`
above it: a Campaign is a fundraising initiative with a goal and a timeline; a `FundraisingEvent`
is a discrete gathering that belongs to a Campaign. One campaign can have multiple events.

This relationship does not exist in the codebase yet. It is the first task of Phase 3.

---

## The layered model — what goes where

This is the most important structural concept in the project. Every entity appears in multiple
forms, and each form has a specific job.

```
Request DTO         → validates input at the API boundary
     ↓
Service model       → carries data through business logic
     ↓
Record (DAO layer)  → maps to DynamoDB; carries persistence annotations
     ↓
Response DTO        → shapes what the client receives
```

| Layer | Class | Lives in | Rule |
|---|---|---|---|
| Request DTO | `CreateEventRequest` | `controller/model/` | Receives client JSON; has `@NotBlank` validation |
| Service model | `Event` | `service/model/` | Pure domain object; no Spring, no DynamoDB annotations |
| Record | `EventRecord` | `repositories/model/` | Has `@DynamoDbBean`, `@DynamoDbPartitionKey` |
| Response DTO | `EventResponse` | `controller/model/` | Shapes what the API returns; hides internal fields |

**Why not just use one class for everything?** Because each layer has different requirements
that conflict. A record needs DynamoDB annotations. A request DTO needs validation annotations.
A response DTO might expose different fields than the internal model stores. Collapsing them
into one class means that class is doing four different jobs, and changing any one of them
breaks all the others.

---

## What is being renamed in Phase 2

| Old name | New name | File(s) affected |
|---|---|---|
| `Customer` | `Attendee` | `Customer.java`, `CustomerTypeConverter.java`, `EventRecord`, `CreateEventRequest`, `EventUpdateRequest`, `EventResponse` |
| `EventUserRepository` | (deleted) | Replaced by `UserDao` ✅ done |
| `ExampleRecord`, `Example`, `ExampleData` | (deleted) | ✅ done |

The `Customer` → `Attendee` rename touches many files because `Customer` is embedded in the
`EventRecord` as a `List<Customer>` and in every request/response DTO. This is a good exercise
in tracing a type change through a layered architecture.

---

## What to understand

1. Why does the `Event` service model class exist separately from `EventRecord`? What would
   break if you removed it and used `EventRecord` directly in `EventService`?
2. The response DTO (`EventResponse`) does not have to match the record (`EventRecord`) field
   for field. What is an example of a field you might store in the record but *not* expose in
   the response?
3. Why is "Attendee" a better domain name than "Customer" for this platform?
4. What is the relationship between `Campaign` and `FundraisingEvent` that the capstone's
   `Event` model cannot represent?

---

## Exercise

→ [Exercise 01 — Rename Customer to Attendee](exercises/01-rename-customer-attendee.md)

## Next

[07 — Caffeine Cache](07-caffeine-cache.md)
