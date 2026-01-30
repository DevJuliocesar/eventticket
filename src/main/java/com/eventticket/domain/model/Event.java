package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.EventId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;

/**
 * Domain entity representing an event.
 * Aggregate root for event management.
 */
@Value
@Builder
@With
public class Event {
    
    EventId eventId;
    String name;
    String description;
    String venue;
    Instant eventDate;
    int totalCapacity;
    int availableTickets;
    int reservedTickets;
    int soldTickets;
    EventStatus status;
    Instant createdAt;
    Instant updatedAt;
    int version;

    /**
     * Creates a new event.
     *
     * @param name Event name
     * @param description Event description
     * @param venue Event venue/location
     * @param eventDate Event date and time
     * @param totalCapacity Total ticket capacity
     * @return A new Event instance
     */
    public static Event create(
            String name,
            String description,
            String venue,
            Instant eventDate,
            int totalCapacity
    ) {
        Instant now = Instant.now();
        
        return Event.builder()
                .eventId(EventId.generate())
                .name(name)
                .description(description)
                .venue(venue)
                .eventDate(eventDate)
                .totalCapacity(totalCapacity)
                .availableTickets(totalCapacity)
                .reservedTickets(0)
                .soldTickets(0)
                .status(EventStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .version(0)
                .build();
    }

    /**
     * Reserves tickets for this event.
     *
     * @param quantity Number of tickets to reserve
     * @return Updated event with reserved tickets
     */
    public Event reserveTickets(int quantity) {
        if (availableTickets < quantity) {
            throw new IllegalStateException(
                    "Not enough tickets available. Requested: " + quantity + 
                    ", Available: " + availableTickets
            );
        }
        
        return this
                .withAvailableTickets(availableTickets - quantity)
                .withReservedTickets(reservedTickets + quantity)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Confirms reserved tickets (converts reservation to sold).
     *
     * @param quantity Number of tickets to confirm
     * @return Updated event
     */
    public Event confirmReservedTickets(int quantity) {
        if (reservedTickets < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: " + reservedTickets + 
                    ", Requested: " + quantity
            );
        }
        
        return this
                .withReservedTickets(reservedTickets - quantity)
                .withSoldTickets(soldTickets + quantity)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Releases reserved tickets back to available.
     *
     * @param quantity Number of tickets to release
     * @return Updated event
     */
    public Event releaseReservedTickets(int quantity) {
        if (reservedTickets < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: " + reservedTickets + 
                    ", Requested: " + quantity
            );
        }
        
        return this
                .withReservedTickets(reservedTickets - quantity)
                .withAvailableTickets(availableTickets + quantity)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Cancels the event.
     *
     * @return Updated event with cancelled status
     */
    public Event cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already cancelled");
        }
        
        return this
                .withStatus(EventStatus.CANCELLED)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Checks if tickets are available.
     *
     * @param quantity Quantity to check
     * @return true if available, false otherwise
     */
    public boolean hasAvailableTickets(int quantity) {
        return availableTickets >= quantity && status == EventStatus.ACTIVE;
    }

    /**
     * Gets total tickets sold (confirmed).
     *
     * @return Number of sold tickets
     */
    public int getTotalSold() {
        return soldTickets;
    }

    /**
     * Gets remaining capacity.
     *
     * @return Number of tickets not yet sold or reserved
     */
    public int getRemainingCapacity() {
        return availableTickets;
    }
}
