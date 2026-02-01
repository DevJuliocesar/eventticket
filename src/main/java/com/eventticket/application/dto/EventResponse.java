package com.eventticket.application.dto;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;

import java.time.Instant;

/**
 * Data Transfer Object for event response.
 * Pure Java Record - Java 25 features.
 */
public record EventResponse(
        String eventId,
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
        Instant updatedAt
) {
    /**
     * Converts domain model to DTO.
     *
     * @param event Domain event
     * @return EventResponse DTO
     */
    public static EventResponse fromDomain(Event event) {
        return new EventResponse(
                event.getEventId().value(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalCapacity(),
                event.getAvailableTickets(),
                event.getReservedTickets(),
                event.getSoldTickets(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
