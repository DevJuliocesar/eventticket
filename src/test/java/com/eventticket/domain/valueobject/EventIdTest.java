package com.eventticket.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventId Value Object Tests")
class EventIdTest {

    @Test
    @DisplayName("Should generate unique EventId")
    void shouldGenerateUniqueEventId() {
        // When
        EventId id1 = EventId.generate();
        EventId id2 = EventId.generate();

        // Then
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    @DisplayName("Should create EventId from string")
    void shouldCreateEventIdFromString() {
        // Given
        String value = "event-123";

        // When
        EventId eventId = EventId.of(value);

        // Then
        assertThat(eventId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("Should throw exception when value is null")
    void shouldThrowExceptionWhenValueIsNull() {
        // When & Then
        assertThatThrownBy(() -> EventId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EventId cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when value is blank")
    void shouldThrowExceptionWhenValueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> EventId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EventId cannot be null or empty");
    }

    @Test
    @DisplayName("Should be equal when values are same")
    void shouldBeEqualWhenValuesAreSame() {
        // Given
        String value = "event-123";
        EventId id1 = EventId.of(value);
        EventId id2 = EventId.of(value);

        // When & Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values are different")
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        EventId id1 = EventId.of("event-123");
        EventId id2 = EventId.of("event-456");

        // When & Then
        assertThat(id1).isNotEqualTo(id2);
    }
}
