package com.eventticket.domain.valueobject;

import java.util.UUID;

/**
 * Value object representing a customer identifier.
 * Using Java Record for immutability.
 */
public record CustomerId(String value) {
    
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CustomerId cannot be null or empty");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
