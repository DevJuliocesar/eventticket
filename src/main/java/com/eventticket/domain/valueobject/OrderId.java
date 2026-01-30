package com.eventticket.domain.valueobject;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing an order identifier.
 */
@Value
public class OrderId {
    String value;

    private OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId cannot be null or empty");
        }
        this.value = value;
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
