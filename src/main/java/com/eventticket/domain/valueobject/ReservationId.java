package com.eventticket.domain.valueobject;

import java.util.UUID;

/**
 * Value object representing a reservation identifier.
 * Using Java Record for immutability.
 */
public record ReservationId(String value) {
    
    public ReservationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReservationId cannot be null or empty");
        }
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
