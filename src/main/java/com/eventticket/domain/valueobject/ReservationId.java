package com.eventticket.domain.valueobject;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a reservation identifier.
 */
@Value
public class ReservationId {
    String value;

    private ReservationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReservationId cannot be null or empty");
        }
        this.value = value;
    }

    public static ReservationId of(String value) {
        return new ReservationId(value);
    }

    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
