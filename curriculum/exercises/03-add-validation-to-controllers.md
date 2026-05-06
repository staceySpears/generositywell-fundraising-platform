# Exercise 03 — Add @Valid to Controllers and Handle Validation Errors

**Covers:** Module 08 — Bean Validation
**Difficulty:** Beginner
**Estimated time:** 30–45 minutes

---

## What you are doing

The DTOs have validation annotations (`@NotBlank`, `@NotEmpty`) but they are not being enforced
because `@Valid` is missing from the controller parameters. You will add `@Valid`, update
`GlobalExceptionHandler` to handle `MethodArgumentNotValidException`, and verify that invalid
requests are rejected before reaching the service.

---

## Before you start

Send a request to `POST /events` with a missing `name` field (use curl or the Swagger UI at
`http://localhost:5001/swagger-ui.html`). Note what the response is. After this exercise,
the same request should return a structured 400 error listing which fields failed.

---

## Steps

**1.** Open `EventController.java`. On the `addEvent` method, add `@Valid` before `@RequestBody`:

```java
public ResponseEntity<EventResponse> addEvent(@Valid @RequestBody CreateEventRequest createEventRequest)
```

Do the same for `updateEvent`. Open `UserController.java` and add `@Valid` to `addNewUser`
and `updateUser`.

**2.** Open `GlobalExceptionHandler.java`. Add a new handler method for validation failures:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + " " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(400, message));
}
```

**3.** Verify the DTO annotations are correct. Open `CreateEventRequest.java`. Check each field:
- String fields that must not be blank: use `@NotBlank`
- The `listOfAttending` field (a `List`): use `@NotNull` (the list can be empty, but not null)
- The `user` field (an object): use `@NotNull`

Fix any annotations that are using `@NotEmpty` on String fields — those should be `@NotBlank`.

**4.** Run the application locally and repeat the test request from "Before you start." The
response should now be:

```json
{
  "status": 400,
  "message": "name must not be blank"
}
```

**5.** Commit:

```
feat: activate bean validation on controllers; add validation error handler
```

---

## Verify your work

- `POST /events` with `{ "name": "" }` returns `400` with a field-level error message
- `POST /events` with a valid body returns `201 Created`
- `POST /users` with a null email returns `400`

---

## What you should be able to explain after this

- Why did `@NotBlank` do nothing before you added `@Valid`?
- `MethodArgumentNotValidException` has a `getBindingResult()` method. What is a `BindingResult`?
- A client sends `{ "name": null, "date": "" }`. How many field errors does the response include?
