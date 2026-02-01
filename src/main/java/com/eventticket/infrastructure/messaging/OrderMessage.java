package com.eventticket.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.eventticket.domain.valueobject.OrderId;

import java.time.Instant;
import java.util.Objects;

/**
 * Message DTO for SQS order processing.
 * Represents an order that needs to be processed asynchronously.
 */
public record OrderMessage(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("ticketType") String ticketType,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("timestamp") Instant timestamp
) {
    @JsonCreator
    public OrderMessage {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(eventId, "Event ID cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(ticketType, "Ticket type cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public static OrderMessage of(OrderId orderId, String eventId, String customerId,
                                   String ticketType, int quantity) {
        return new OrderMessage(
                orderId.value(),
                eventId,
                customerId,
                ticketType,
                quantity,
                Instant.now()
        );
    }
}
