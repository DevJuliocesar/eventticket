package com.eventticket.infrastructure.api;

import com.eventticket.domain.exception.DomainException;
import com.eventticket.domain.exception.InsufficientInventoryException;
import com.eventticket.domain.exception.InvalidTicketStateTransitionException;
import com.eventticket.domain.exception.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the REST API.
 * Centralizes error handling following the DRY principle.
 * Using Java 25 Records for response DTOs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleOrderNotFoundException(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Order not found",
                        ex.getMessage()
                )));
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInsufficientInventoryException(
            InsufficientInventoryException ex
    ) {
        log.warn("Insufficient inventory: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "Insufficient inventory",
                        ex.getMessage()
                )));
    }

    @ExceptionHandler(InvalidTicketStateTransitionException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidTicketStateTransitionException(
            InvalidTicketStateTransitionException ex
    ) {
        log.warn("Invalid ticket state transition: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid ticket state transition",
                        ex.getMessage()
                )));
    }

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDomainException(DomainException ex) {
        log.error("Domain exception: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Business rule violation",
                        ex.getMessage()
                )));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ValidationErrorResponse>> handleValidationException(
            WebExchangeBindException ex
    ) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                // Java 25 Pattern Matching for instanceof
                errors.put(fieldError.getField(), error.getDefaultMessage());
            }
        });

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation failed",
                        errors
                )));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid argument",
                        ex.getMessage()
                )));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal server error",
                        "An unexpected error occurred"
                )));
    }

    /**
     * Error response record - Java 25 style.
     */
    public record ErrorResponse(
            int status,
            String error,
            String message,
            Instant timestamp
    ) {
        public static ErrorResponse of(int status, String error, String message) {
            return new ErrorResponse(status, error, message, Instant.now());
        }
    }

    /**
     * Validation error response record - Java 25 style.
     */
    public record ValidationErrorResponse(
            int status,
            String error,
            Map<String, String> validationErrors,
            Instant timestamp
    ) {
        public static ValidationErrorResponse of(int status, String error, Map<String, String> errors) {
            return new ValidationErrorResponse(status, error, errors, Instant.now());
        }
    }
}
