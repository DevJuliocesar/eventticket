package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.EventId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an event.
 * Aggregate root for event management.
 * Using Java 25 - immutable class with wither pattern.
 */
public final class Event {
    
    private final EventId eventId;
    private final String name;
    private final String description;
    private final String venue;
    private final Instant eventDate;
    private final int totalCapacity;
    private final int availableTickets;
    private final int reservedTickets;
    private final int soldTickets;
    private final EventStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int version;

    private Event(
            EventId eventId,
            String name,
            String description,
            String venue,
            Instant eventDate,
            int totalCapacity,
            int availableTickets,
            int reservedTickets,
            int soldTickets,
            EventStatus status,
            Instant createdAt,
            Instant updatedAt,
            int version
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.venue = Objects.requireNonNull(venue);
        this.eventDate = Objects.requireNonNull(eventDate);
        this.totalCapacity = totalCapacity;
        this.availableTickets = availableTickets;
        this.reservedTickets = reservedTickets;
        this.soldTickets = soldTickets;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.version = version;
    }

    /**
     * Creates a new event.
     */
    public static Event create(
            String name,
            String description,
            String venue,
            Instant eventDate,
            int totalCapacity
    ) {
        Instant now = Instant.now();
        
        return new Event(
                EventId.generate(),
                name,
                description,
                venue,
                eventDate,
                totalCapacity,
                totalCapacity,
                0,
                0,
                EventStatus.ACTIVE,
                now,
                now,
                0
        );
    }

    /**
     * Reserves tickets for this event.
     */
    public Event reserveTickets(int quantity) {
        if (availableTickets < quantity) {
            throw new IllegalStateException(
                    "Not enough tickets available. Requested: %d, Available: %d"
                            .formatted(quantity, availableTickets)
            );
        }
        
        return new Event(eventId, name, description, venue, eventDate, totalCapacity,
                availableTickets - quantity, reservedTickets + quantity, soldTickets,
                status, createdAt, Instant.now(), version + 1);
    }

    /**
     * Confirms reserved tickets (converts reservation to sold).
     */
    public Event confirmReservedTickets(int quantity) {
        if (reservedTickets < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: %d, Requested: %d"
                            .formatted(reservedTickets, quantity)
            );
        }
        
        return new Event(eventId, name, description, venue, eventDate, totalCapacity,
                availableTickets, reservedTickets - quantity, soldTickets + quantity,
                status, createdAt, Instant.now(), version + 1);
    }

    /**
     * Releases reserved tickets back to available.
     */
    public Event releaseReservedTickets(int quantity) {
        if (reservedTickets < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: %d, Requested: %d"
                            .formatted(reservedTickets, quantity)
            );
        }
        
        return new Event(eventId, name, description, venue, eventDate, totalCapacity,
                availableTickets + quantity, reservedTickets - quantity, soldTickets,
                status, createdAt, Instant.now(), version + 1);
    }

    /**
     * Cancels the event.
     */
    public Event cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already cancelled");
        }
        
        return new Event(eventId, name, description, venue, eventDate, totalCapacity,
                availableTickets, reservedTickets, soldTickets,
                EventStatus.CANCELLED, createdAt, Instant.now(), version + 1);
    }

    /**
     * Checks if tickets are available.
     */
    public boolean hasAvailableTickets(int quantity) {
        return availableTickets >= quantity && status == EventStatus.ACTIVE;
    }

    public int getTotalSold() { return soldTickets; }
    public int getRemainingCapacity() { return availableTickets; }

    // Getters
    public EventId getEventId() { return eventId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public Instant getEventDate() { return eventDate; }
    public int getTotalCapacity() { return totalCapacity; }
    public int getAvailableTickets() { return availableTickets; }
    public int getReservedTickets() { return reservedTickets; }
    public int getSoldTickets() { return soldTickets; }
    public EventStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Event other && Objects.equals(eventId, other.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "Event[eventId=%s, name=%s, status=%s]".formatted(eventId, name, status);
    }
}
