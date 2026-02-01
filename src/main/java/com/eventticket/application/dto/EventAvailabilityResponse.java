package com.eventticket.application.dto;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;

/**
 * Data Transfer Object for event availability response.
 * Functional Requirement #7: Real-time availability query.
 * Pure Java Record - Java 25 features.
 */
public record EventAvailabilityResponse(
        String eventId,
        String name,
        int totalCapacity,
        int availableTickets,
        int reservedTickets,
        int soldTickets,
        EventStatus status,
        boolean hasAvailability
) {
    /**
     * Converts domain model to availability DTO.
     *
     * @param event Domain event
     * @return EventAvailabilityResponse DTO
     */
    public static EventAvailabilityResponse fromDomain(Event event) {
        return new EventAvailabilityResponse(
                event.getEventId().value(),
                event.getName(),
                event.getTotalCapacity(),
                event.getAvailableTickets(),
                event.getReservedTickets(),
                event.getSoldTickets(),
                event.getStatus(),
                event.hasAvailableTickets(1)
        );
    }
}
