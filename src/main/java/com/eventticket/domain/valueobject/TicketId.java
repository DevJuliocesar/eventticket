package com.eventticket.domain.valueobject;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a ticket identifier.
 */
@Value
public class TicketId {
    String value;

    private TicketId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TicketId cannot be null or empty");
        }
        this.value = value;
    }

    public static TicketId of(String value) {
        return new TicketId(value);
    }

    public static TicketId generate() {
        return new TicketId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
