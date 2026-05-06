# 08 — Bean Validation: Protecting the API Boundary

## Why this exists

Validation is about trust. Inside your application — between the service, the DAO, and the
domain model — you trust the data. It came from your own code. If `EventService` calls
`eventDao.save(record)`, you do not re-validate every field of `record` because you know
`EventService` constructed it correctly.

At the *boundary* — where external input enters your system — you do not trust the data.
An HTTP request body can contain anything: null fields, empty strings, malformed dates, strings
where numbers are expected. Bean validation is the mechanism that enforces a contract at
that boundary before your business logic ever runs.

The capstone imported `javax.validation.constraints.@NotEmpty` on the DTO fields but never
activated validation — there was no `@Valid` annotation on the controller parameters, so
the annotations were decorative, doing nothing at runtime.

---

## The code

### The wrong way — what the capstone had

```java
// CreateEventRequest.java
public class CreateEventRequest {
    @NotEmpty
    @JsonProperty("name")
    private String name;

    @NotEmpty
    @JsonProperty("date")
    private String date;
    // ...
}

// EventController.java
@PostMapping
public ResponseEntity<EventResponse> addEvent(@RequestBody CreateEventRequest createEventRequest) {
    // (1) No @Valid here — the @NotEmpty annotations above do nothing
    EventResponse eventResponse = eventService.addNewEvent(createEventRequest);
    return ResponseEntity.ok(eventResponse);
}
```

**(1)** Without `@Valid` on `@RequestBody`, Spring never invokes the validator. The annotations
compile and appear in the code, but no validation runs. You could send `{ "name": "" }` and
the request would proceed to the service.

---

### The right way — activating validation

```java
// EventController.java
@PostMapping
public ResponseEntity<EventResponse> addEvent(@Valid @RequestBody CreateEventRequest createEventRequest) {
    EventResponse eventResponse = eventService.addNewEvent(createEventRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(eventResponse);
}
```

Adding `@Valid` tells Spring to run the Jakarta Validation engine against the request object
before calling your method. If any constraint fails, Spring throws a `MethodArgumentNotValidException`
before your service is ever called.

You catch that in `GlobalExceptionHandler`:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + " " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(400, message));
}
```

This returns a structured error that tells the client *which fields* failed and *why*.

---

### Choosing the right constraint

| Annotation | Fails when | Use for |
|---|---|---|
| `@NotNull` | Value is null | Object references; IDs |
| `@NotBlank` | Value is null, empty, or whitespace-only | Strings (name, email, description) |
| `@NotEmpty` | Value is null or empty (not whitespace-aware) | Collections, arrays |
| `@Size(min, max)` | String length or collection size outside bounds | Names, descriptions |
| `@Email` | Value does not match email format | Email fields |
| `@Positive` | Value is zero or negative | Monetary amounts, quantities |

`@NotEmpty` on a `String` field passes `" "` (a space). That is almost never what you want.
Use `@NotBlank` for string fields.

---

### Where NOT to validate

Do not add validation annotations to:

- **Service model classes** (`Event`, `User`) — these are constructed by your own code, not
  from external input. Validating them adds noise and false security.
- **Record classes** (`EventRecord`) — DynamoDB records are written and read by your own
  persistence layer. External input never touches them directly.
- **Response DTOs** (`EventResponse`) — you control what goes into the response; you do not
  need to validate your own output.

Validation belongs at the boundary. One boundary, one validation layer.

---

## What to understand

1. Why does `@NotEmpty` on a controller parameter do nothing without `@Valid`?
2. What is the difference between `@NotNull`, `@NotEmpty`, and `@NotBlank`?
3. `MethodArgumentNotValidException` is thrown when validation fails. Which class in this
   project catches it?
4. Why should you NOT add `@NotBlank` to the fields of `EventRecord`?

---

## Next

[09 — Campaign Entity and Status Lifecycle](09-campaign-entity.md)
