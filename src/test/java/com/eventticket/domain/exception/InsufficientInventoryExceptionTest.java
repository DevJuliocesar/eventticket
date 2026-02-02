package com.eventticket.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InsufficientInventoryException Tests")
class InsufficientInventoryExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Not enough tickets available";

        // When
        InsufficientInventoryException exception = new InsufficientInventoryException(message);

        // Then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Not enough tickets available";
        Throwable cause = new RuntimeException("Root cause");

        // When
        InsufficientInventoryException exception = new InsufficientInventoryException(message, cause);

        // Then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
