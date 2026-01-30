package com.eventticket.domain.valueobject;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing an event identifier.
 */
@Value
public class EventId {
    String value;

    private EventId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EventId cannot be null or empty");
        }
        this.value = value;
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
