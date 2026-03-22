package com.kenzie.appserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
   * Replaces scattered ResponseStatusException throws and ad-hoc null checks.
   */
@RestControllerAdvice
  public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Spring's built-in ResponseStatusException (e.g., HttpStatus.BAD_REQUEST, NOT_FOUND).
       */
    @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
                  log.warn("ResponseStatusException: status={}, reason={}", ex.getStatusCode(), ex.getReason());
                  return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        }

    /**
     * Handles @Valid / @Validated constraint violations on @RequestBody DTOs.
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
     * Handles illegal argument exceptions thrown by the service layer (e.g., duplicate id).
         */
    @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
                  log.warn("IllegalArgumentException: {}", ex.getMessage());
                  return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

    /**
     * Catch-all handler for any uncaught runtime exceptions.
         * Returns 500 Internal Server Error without leaking stack trace details.
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
