package com.eventticket.domain.valueobject;

import java.util.UUID;

/**
 * Value object representing an order identifier.
 * Using Java Record for immutability and automatic equals/hashCode/toString.
 */
public record OrderId(String value) {
    
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId cannot be null or empty");
        }
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
