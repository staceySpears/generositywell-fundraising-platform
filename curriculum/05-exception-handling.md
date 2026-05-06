# 05 — Global Exception Handling: Consistent Error Responses Across All Endpoints

## Why this exists

Without centralized exception handling, every controller method has to catch its own exceptions
and decide what to return. You end up with inconsistent error shapes — one endpoint returns
`{ "message": "not found" }`, another returns `{ "error": "Event not found" }`, another returns
an empty 500 with a Java stack trace. Frontend developers, API clients, and monitoring tools
all have to handle each endpoint differently.

`@RestControllerAdvice` gives you one place to define what every exception looks like over
the wire. When a service throws `ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")`,
the handler catches it and returns the same JSON shape as every other 404 in the system.

This is not just about cleanliness. It is a security concern: unhandled exceptions in Spring
Boot will return a default error response that includes the stack trace in some configurations.
Stack traces leak internal details — package names, library versions, internal method names —
that attackers use for reconnaissance.

---

## The code

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice                          // (1)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex) {

        ErrorResponse body = new ErrorResponse(
                ex.getStatusCode().value(),    // (2)
                ex.getReason()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)         // (3)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse body = new ErrorResponse(500, "An unexpected error occurred");
        return ResponseEntity.internalServerError().body(body);
    }
}
```

**(1)** `@RestControllerAdvice` — applies to all `@RestController` classes in the application
context. Any exception thrown from a controller method (or the service it calls) that is not
caught locally will flow here.

**(2)** `ex.getStatusCode().value()` — uses the status code the service set when it threw
`ResponseStatusException`. The handler does not override the status; it trusts the service
to set the right one and just formats the response consistently.

**(3)** The generic `Exception` handler is the safety net. Any unchecked exception that is
not a `ResponseStatusException` gets a generic 500 response. Notice the message is
intentionally vague — we do not expose the exception's `getMessage()` to the client.

---

### HTTP status codes — getting them right

The old capstone code used `HttpStatus.BAD_REQUEST` (400) for almost everything:

```java
// Old — wrong status for every failure mode
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event not found");
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid User request.");
```

400 means the *client's request was malformed*. Missing a required field is a 400. Sending
a string where a number is expected is a 400. "The record you asked for does not exist" is
not the client's fault — that is a 404.

The refactored `EventService` uses the correct codes:

```java
// Not found — the record does not exist
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");

// Forbidden — the record exists but this user cannot modify it
throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event creator can update this event");

// Bad request — the input itself is invalid
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID cannot be empty");
```

Status codes matter for clients. A frontend application that receives a 404 knows to show a
"not found" message. A 403 means "you are logged in, but you do not have permission" — redirect
to an access denied page. A 400 means "fix your request and try again." Collapsing all of these
into 400 forces every client to parse the error message string instead of using the status code,
which is exactly what status codes exist to avoid.

---

### The standard error response shape

```java
public record ErrorResponse(int status, String message) {}
```

Using a Java record for the response body means no boilerplate: the constructor, `status()`,
`message()`, `equals()`, `hashCode()`, and `toString()` are all generated. Jackson serializes
it to:

```json
{
  "status": 404,
  "message": "Event not found"
}
```

Every error in the system — regardless of which controller or service threw — returns this shape.

---

## What to understand

1. What is the difference between `@ControllerAdvice` and `@RestControllerAdvice`?
2. If a service throws a `NullPointerException`, which handler catches it — the
   `ResponseStatusException` handler or the generic `Exception` handler? Why?
3. Why should the generic `Exception` handler return `"An unexpected error occurred"` rather
   than `ex.getMessage()`?
4. An endpoint returns `400 Bad Request` when the requested resource does not exist. What is
   wrong with that, and what should it return instead?
5. What is a Java record, and why is it a better choice than a class for a DTO that just
   carries data?

---

## Next

[06 — Domain Modeling and Naming](06-domain-modeling.md)
