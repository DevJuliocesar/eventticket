package com.eventticket.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReservationId Tests")
class ReservationIdTest {

    @Test
    @DisplayName("Should create ReservationId from string")
    void shouldCreateReservationIdFromString() {
        // Given
        String value = "reservation-123";

        // When
        ReservationId reservationId = ReservationId.of(value);

        // Then
        assertThat(reservationId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("Should generate unique ReservationId")
    void shouldGenerateUniqueReservationId() {
        // When
        ReservationId id1 = ReservationId.generate();
        ReservationId id2 = ReservationId.generate();

        // Then
        assertThat(id1.value()).isNotBlank();
        assertThat(id2.value()).isNotBlank();
        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    @DisplayName("Should throw exception when value is null")
    void shouldThrowExceptionWhenValueIsNull() {
        // When & Then
        assertThatThrownBy(() -> ReservationId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when value is blank")
    void shouldThrowExceptionWhenValueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> ReservationId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");

        assertThatThrownBy(() -> ReservationId.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("Should return value in toString")
    void shouldReturnValueInToString() {
        // Given
        String value = "reservation-456";
        ReservationId reservationId = ReservationId.of(value);

        // When
        String result = reservationId.toString();

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("Should be equal when values are equal")
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "reservation-789";
        ReservationId id1 = ReservationId.of(value);
        ReservationId id2 = ReservationId.of(value);

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values are different")
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        ReservationId id1 = ReservationId.of("reservation-1");
        ReservationId id2 = ReservationId.of("reservation-2");

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }
}
