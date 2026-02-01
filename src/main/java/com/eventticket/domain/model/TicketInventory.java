package com.eventticket.domain.model;

import com.eventticket.domain.exception.InsufficientInventoryException;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;

import java.util.Objects;

/**
 * Domain entity representing ticket inventory for an event.
 * Implements optimistic locking through version field.
 * Using Java 25 - immutable class with wither pattern.
 */
public final class TicketInventory {
    
    private final EventId eventId;
    private final String ticketType;
    private final String eventName;
    private final int totalQuantity;
    private final int availableQuantity;
    private final int reservedQuantity;
    private final Money price;
    private final int version;

    private TicketInventory(
            EventId eventId,
            String ticketType,
            String eventName,
            int totalQuantity,
            int availableQuantity,
            int reservedQuantity,
            Money price,
            int version
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.ticketType = Objects.requireNonNull(ticketType);
        this.eventName = Objects.requireNonNull(eventName);
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.price = Objects.requireNonNull(price);
        this.version = version;
    }

    /**
     * Creates a new ticket inventory.
     */
    public static TicketInventory create(
            EventId eventId,
            String ticketType,
            String eventName,
            int totalQuantity,
            Money price
    ) {
        return new TicketInventory(
                eventId,
                ticketType,
                eventName,
                totalQuantity,
                totalQuantity,
                0,
                price,
                0
        );
    }

    /**
     * Reserves tickets from inventory.
     */
    public TicketInventory reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new InsufficientInventoryException(
                    "Not enough tickets available. Requested: %d, Available: %d"
                            .formatted(quantity, availableQuantity)
            );
        }
        
        return new TicketInventory(eventId, ticketType, eventName, totalQuantity,
                availableQuantity - quantity, reservedQuantity + quantity,
                price, version + 1);
    }

    /**
     * Confirms reserved tickets (converts reservation to sold).
     */
    public TicketInventory confirmReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: %d, Requested: %d"
                            .formatted(reservedQuantity, quantity)
            );
        }
        
        return new TicketInventory(eventId, ticketType, eventName, totalQuantity,
                availableQuantity, reservedQuantity - quantity,
                price, version + 1);
    }

    /**
     * Releases reserved tickets back to available inventory.
     */
    public TicketInventory releaseReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: %d, Requested: %d"
                            .formatted(reservedQuantity, quantity)
            );
        }
        
        return new TicketInventory(eventId, ticketType, eventName, totalQuantity,
                availableQuantity + quantity, reservedQuantity - quantity,
                price, version + 1);
    }

    /**
     * Checks if the requested quantity is available.
     */
    public boolean isAvailable(int quantity) {
        return availableQuantity >= quantity;
    }

    // Getters
    public EventId getEventId() { return eventId; }
    public String getTicketType() { return ticketType; }
    public String getEventName() { return eventName; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public Money getPrice() { return price; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        return o instanceof TicketInventory other 
                && Objects.equals(eventId, other.eventId)
                && Objects.equals(ticketType, other.ticketType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, ticketType);
    }

    @Override
    public String toString() {
        return "TicketInventory[eventId=%s, type=%s, available=%d]"
                .formatted(eventId, ticketType, availableQuantity);
    }
}
