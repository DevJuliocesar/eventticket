package com.eventticket.infrastructure.api;

import com.eventticket.domain.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("Should handle OrderNotFoundException with 404")
    void shouldHandleOrderNotFoundException() {
        // Given
        OrderNotFoundException ex = new OrderNotFoundException("order-123");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleOrderNotFoundException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().status()).isEqualTo(404);
                    assertThat(response.getBody().error()).isEqualTo("Order not found");
                    assertThat(response.getBody().message()).contains("order-123");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle InsufficientInventoryException with 409")
    void shouldHandleInsufficientInventoryException() {
        // Given
        InsufficientInventoryException ex = new InsufficientInventoryException("Not enough tickets");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleInsufficientInventoryException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(response.getBody().status()).isEqualTo(409);
                    assertThat(response.getBody().error()).isEqualTo("Insufficient inventory");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle InvalidTicketStateTransitionException with 400")
    void shouldHandleInvalidTicketStateTransitionException() {
        // Given
        InvalidTicketStateTransitionException ex = 
                new InvalidTicketStateTransitionException("Invalid transition");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleInvalidTicketStateTransitionException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody().status()).isEqualTo(400);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle DomainException with 400")
    void shouldHandleDomainException() {
        // Given
        DomainException ex = new DomainException("Business rule violation");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleDomainException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody().status()).isEqualTo(400);
                    assertThat(response.getBody().error()).isEqualTo("Business rule violation");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400")
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleIllegalArgumentException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody().status()).isEqualTo(400);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle IllegalStateException with 409")
    void shouldHandleIllegalStateException() {
        // Given
        IllegalStateException ex = new IllegalStateException("Illegal state");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleIllegalStateException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(response.getBody().status()).isEqualTo(409);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with 404")
    void shouldHandleResponseStatusException404() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Resource not found"
        );

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleResponseStatusException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody().status()).isEqualTo(404);
                    assertThat(response.getBody().error()).isEqualTo("Not Found");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle generic Exception with 500")
    void shouldHandleGenericException() {
        // Given
        Exception ex = new RuntimeException("Unexpected error");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleGenericException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody().status()).isEqualTo(500);
                    assertThat(response.getBody().error()).isEqualTo("Internal server error");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should not handle ResponseStatusException in generic handler")
    void shouldNotHandleResponseStatusExceptionInGenericHandler() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Not found"
        );

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ErrorResponse>> result =
                exceptionHandler.handleGenericException(ex);

        // Then
        StepVerifier.create(result)
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle WebExchangeBindException with validation errors")
    void shouldHandleWebExchangeBindExceptionWithValidationErrors() {
        // Given
        org.springframework.validation.BindingResult bindingResult = 
                org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fieldError1 = 
                new org.springframework.validation.FieldError(
                        "createEventRequest", "name", "Event name cannot be blank");
        org.springframework.validation.FieldError fieldError2 = 
                new org.springframework.validation.FieldError(
                        "createEventRequest", "eventDate", "Event date must be in the future");
        
        java.util.List<org.springframework.validation.ObjectError> allErrors = 
                java.util.List.of(fieldError1, fieldError2);
        
        org.mockito.Mockito.when(bindingResult.getAllErrors())
                .thenReturn(allErrors);
        
        WebExchangeBindException ex = org.mockito.Mockito.mock(WebExchangeBindException.class);
        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(ex.getMessage()).thenReturn("Validation failed");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse>> result =
                exceptionHandler.handleValidationException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().status()).isEqualTo(400);
                    assertThat(response.getBody().error()).isEqualTo("Validation failed");
                    assertThat(response.getBody().validationErrors()).hasSize(2);
                    assertThat(response.getBody().validationErrors().get("name"))
                            .isEqualTo("Event name cannot be blank");
                    assertThat(response.getBody().validationErrors().get("eventDate"))
                            .isEqualTo("Event date must be in the future");
                    assertThat(response.getBody().timestamp()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle WebExchangeBindException with empty errors")
    void shouldHandleWebExchangeBindExceptionWithEmptyErrors() {
        // Given
        org.springframework.validation.BindingResult bindingResult = 
                org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        org.mockito.Mockito.when(bindingResult.getAllErrors())
                .thenReturn(java.util.Collections.emptyList());
        
        WebExchangeBindException ex = org.mockito.Mockito.mock(WebExchangeBindException.class);
        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(ex.getMessage()).thenReturn("Validation failed");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse>> result =
                exceptionHandler.handleValidationException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().validationErrors()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle WebExchangeBindException with non-FieldError errors")
    void shouldHandleWebExchangeBindExceptionWithNonFieldErrorErrors() {
        // Given
        org.springframework.validation.BindingResult bindingResult = 
                org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.ObjectError objectError = 
                new org.springframework.validation.ObjectError(
                        "createEventRequest", "Global validation error");
        
        org.mockito.Mockito.when(bindingResult.getAllErrors())
                .thenReturn(java.util.List.of(objectError));
        
        WebExchangeBindException ex = org.mockito.Mockito.mock(WebExchangeBindException.class);
        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(ex.getMessage()).thenReturn("Validation failed");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse>> result =
                exceptionHandler.handleValidationException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    // Non-FieldError errors are not added to the map
                    assertThat(response.getBody().validationErrors()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should create ValidationErrorResponse with of method")
    void shouldCreateValidationErrorResponseWithOfMethod() {
        // Given
        int status = 400;
        String error = "Validation failed";
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        errors.put("name", "Name is required");
        errors.put("email", "Email is invalid");

        // When
        GlobalExceptionHandler.ValidationErrorResponse response =
                GlobalExceptionHandler.ValidationErrorResponse.of(status, error, errors);

        // Then
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.error()).isEqualTo(error);
        assertThat(response.validationErrors()).isEqualTo(errors);
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.validationErrors()).hasSize(2);
    }

    @Test
    @DisplayName("Should create ValidationErrorResponse with empty errors map")
    void shouldCreateValidationErrorResponseWithEmptyErrorsMap() {
        // Given
        int status = 400;
        String error = "Validation failed";
        java.util.Map<String, String> errors = new java.util.HashMap<>();

        // When
        GlobalExceptionHandler.ValidationErrorResponse response =
                GlobalExceptionHandler.ValidationErrorResponse.of(status, error, errors);

        // Then
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.error()).isEqualTo(error);
        assertThat(response.validationErrors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create ValidationErrorResponse with multiple field errors")
    void shouldCreateValidationErrorResponseWithMultipleFieldErrors() {
        // Given
        org.springframework.validation.BindingResult bindingResult = 
                org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        java.util.List<org.springframework.validation.FieldError> fieldErrors = java.util.List.of(
                new org.springframework.validation.FieldError(
                        "request", "field1", "Field 1 error"),
                new org.springframework.validation.FieldError(
                        "request", "field2", "Field 2 error"),
                new org.springframework.validation.FieldError(
                        "request", "field3", "Field 3 error")
        );
        
        org.mockito.Mockito.when(bindingResult.getAllErrors())
                .thenReturn(new java.util.ArrayList<>(fieldErrors));
        
        WebExchangeBindException ex = org.mockito.Mockito.mock(WebExchangeBindException.class);
        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(ex.getMessage()).thenReturn("Multiple validation errors");

        // When
        Mono<ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse>> result =
                exceptionHandler.handleValidationException(ex);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().validationErrors()).hasSize(3);
                    assertThat(response.getBody().validationErrors().get("field1")).isEqualTo("Field 1 error");
                    assertThat(response.getBody().validationErrors().get("field2")).isEqualTo("Field 2 error");
                    assertThat(response.getBody().validationErrors().get("field3")).isEqualTo("Field 3 error");
                })
                .verifyComplete();
    }
}
