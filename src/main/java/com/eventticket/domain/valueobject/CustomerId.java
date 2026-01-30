package com.eventticket.domain.valueobject;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a customer identifier.
 */
@Value
public class CustomerId {
    String value;

    private CustomerId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CustomerId cannot be null or empty");
        }
        this.value = value;
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
