package com.eventticket.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderNotFoundException Tests")
class OrderNotFoundExceptionTest {

    @Test
    @DisplayName("Should create exception with orderId")
    void shouldCreateExceptionWithOrderId() {
        // Given
        String orderId = "order-123";

        // When
        OrderNotFoundException exception = new OrderNotFoundException(orderId);

        // Then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo("Order not found: " + orderId);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with orderId and cause")
    void shouldCreateExceptionWithOrderIdAndCause() {
        // Given
        String orderId = "order-456";
        Throwable cause = new RuntimeException("Root cause");

        // When
        OrderNotFoundException exception = new OrderNotFoundException(orderId, cause);

        // Then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo("Order not found: " + orderId);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
