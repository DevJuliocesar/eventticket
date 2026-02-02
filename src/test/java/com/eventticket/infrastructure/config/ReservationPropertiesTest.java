package com.eventticket.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReservationProperties Tests")
class ReservationPropertiesTest {

    private ReservationProperties reservationProperties;

    @BeforeEach
    void setUp() {
        reservationProperties = new ReservationProperties();
    }

    @Test
    @DisplayName("Should have default timeout minutes")
    void shouldHaveDefaultTimeoutMinutes() {
        // Then
        assertThat(reservationProperties.getTimeoutMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should have default check interval")
    void shouldHaveDefaultCheckInterval() {
        // Then
        assertThat(reservationProperties.getCheckIntervalMs()).isEqualTo(60000);
    }

    @Test
    @DisplayName("Should set and get timeout minutes")
    void shouldSetAndGetTimeoutMinutes() {
        // Given
        int timeoutMinutes = 15;

        // When
        reservationProperties.setTimeoutMinutes(timeoutMinutes);

        // Then
        assertThat(reservationProperties.getTimeoutMinutes()).isEqualTo(timeoutMinutes);
    }

    @Test
    @DisplayName("Should set and get check interval")
    void shouldSetAndGetCheckInterval() {
        // Given
        long checkIntervalMs = 30000;

        // When
        reservationProperties.setCheckIntervalMs(checkIntervalMs);

        // Then
        assertThat(reservationProperties.getCheckIntervalMs()).isEqualTo(checkIntervalMs);
    }

    @Test
    @DisplayName("Should accept zero timeout minutes")
    void shouldAcceptZeroTimeoutMinutes() {
        // When
        reservationProperties.setTimeoutMinutes(0);

        // Then
        assertThat(reservationProperties.getTimeoutMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should accept negative timeout minutes")
    void shouldAcceptNegativeTimeoutMinutes() {
        // When
        reservationProperties.setTimeoutMinutes(-1);

        // Then
        assertThat(reservationProperties.getTimeoutMinutes()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should accept zero check interval")
    void shouldAcceptZeroCheckInterval() {
        // When
        reservationProperties.setCheckIntervalMs(0);

        // Then
        assertThat(reservationProperties.getCheckIntervalMs()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should accept negative check interval")
    void shouldAcceptNegativeCheckInterval() {
        // When
        reservationProperties.setCheckIntervalMs(-1);

        // Then
        assertThat(reservationProperties.getCheckIntervalMs()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should accept large timeout minutes")
    void shouldAcceptLargeTimeoutMinutes() {
        // When
        reservationProperties.setTimeoutMinutes(Integer.MAX_VALUE);

        // Then
        assertThat(reservationProperties.getTimeoutMinutes()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Should accept large check interval")
    void shouldAcceptLargeCheckInterval() {
        // When
        reservationProperties.setCheckIntervalMs(Long.MAX_VALUE);

        // Then
        assertThat(reservationProperties.getCheckIntervalMs()).isEqualTo(Long.MAX_VALUE);
    }
}
