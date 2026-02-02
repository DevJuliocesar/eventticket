package com.eventticket.infrastructure.api;

import com.eventticket.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
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
}
