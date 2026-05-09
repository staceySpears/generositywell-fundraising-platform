package com.kenzie.appserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Application REST API.
 * Centralizes error response formatting and logging across all controllers.
 * Produces a consistent JSON body: {@code timestamp}, {@code status}, {@code error}, {@code message}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Spring's {@link ResponseStatusException} thrown by service and controller layers.
     *
     * @param ex the exception carrying the HTTP status and reason phrase
     * @return a structured error response with the exception's status code
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
    }

    /**
     * Handles {@code @Valid} / {@code @Validated} constraint violations on {@code @RequestBody} DTOs.
     * Collects all field-level errors into a single comma-separated message.
     *
     * @param ex the binding exception containing all field errors
     * @return 400 with a message listing every failing field and constraint
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", fieldErrors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + fieldErrors);
    }

    /**
     * Handles malformed or missing request bodies (e.g., null body, invalid JSON syntax).
     *
     * @param ex the parse exception
     * @return 400 with a generic unreadable-body message (no internal detail leaked)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Request body is missing or malformed");
    }

    /**
     * Handles {@link IllegalArgumentException} thrown by the service layer.
     *
     * @param ex the exception
     * @return 400 with the exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Catch-all handler for any uncaught runtime exceptions.
     * Returns 500 without leaking stack trace details to the client.
     *
     * @param ex the unhandled exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
