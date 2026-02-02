package com.eventticket.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketId Tests")
class TicketIdTest {

    @Test
    @DisplayName("Should create TicketId from string")
    void shouldCreateTicketIdFromString() {
        // Given
        String value = "ticket-123";

        // When
        TicketId ticketId = TicketId.of(value);

        // Then
        assertThat(ticketId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("Should generate unique TicketId")
    void shouldGenerateUniqueTicketId() {
        // When
        TicketId id1 = TicketId.generate();
        TicketId id2 = TicketId.generate();

        // Then
        assertThat(id1.value()).isNotBlank();
        assertThat(id2.value()).isNotBlank();
        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    @DisplayName("Should throw exception when value is null")
    void shouldThrowExceptionWhenValueIsNull() {
        // When & Then
        assertThatThrownBy(() -> TicketId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when value is blank")
    void shouldThrowExceptionWhenValueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> TicketId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");

        assertThatThrownBy(() -> TicketId.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("Should return value in toString")
    void shouldReturnValueInToString() {
        // Given
        String value = "ticket-456";
        TicketId ticketId = TicketId.of(value);

        // When
        String result = ticketId.toString();

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("Should be equal when values are equal")
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "ticket-789";
        TicketId id1 = TicketId.of(value);
        TicketId id2 = TicketId.of(value);

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values are different")
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        TicketId id1 = TicketId.of("ticket-1");
        TicketId id2 = TicketId.of("ticket-2");

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }
}
