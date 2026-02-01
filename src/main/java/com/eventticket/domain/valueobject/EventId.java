package com.eventticket.domain.valueobject;

import java.util.UUID;

/**
 * Value object representing an event identifier.
 * Using Java Record for immutability.
 */
public record EventId(String value) {
    
    public EventId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EventId cannot be null or empty");
        }
    }

    public static EventId of(String value) {
        return new EventId(value);
    }

    public static EventId generate() {
        return new EventId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
