package com.eventticket.application.dto;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import lombok.Builder;

/**
 * Data Transfer Object for event availability response.
 * Functional Requirement #7: Real-time availability query.
 */
@Builder
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
        return EventAvailabilityResponse.builder()
                .eventId(event.getEventId().getValue())
                .name(event.getName())
                .totalCapacity(event.getTotalCapacity())
                .availableTickets(event.getAvailableTickets())
                .reservedTickets(event.getReservedTickets())
                .soldTickets(event.getSoldTickets())
                .status(event.getStatus())
                .hasAvailability(event.hasAvailableTickets(1))
                .build();
    }
}
