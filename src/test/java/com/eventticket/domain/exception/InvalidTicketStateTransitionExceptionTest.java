package com.eventticket.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidTicketStateTransitionException Tests")
class InvalidTicketStateTransitionExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Invalid state transition from AVAILABLE to SOLD";

        // When
        InvalidTicketStateTransitionException exception = new InvalidTicketStateTransitionException(message);

        // Then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Invalid state transition";
        Throwable cause = new RuntimeException("Root cause");

        // When
        InvalidTicketStateTransitionException exception = new InvalidTicketStateTransitionException(message, cause);

        // Then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
