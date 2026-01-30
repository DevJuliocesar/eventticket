package com.eventticket.application.dto;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import lombok.Builder;

import java.time.Instant;

/**
 * Data Transfer Object for event response.
 */
@Builder
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
        return EventResponse.builder()
                .eventId(event.getEventId().getValue())
                .name(event.getName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .totalCapacity(event.getTotalCapacity())
                .availableTickets(event.getAvailableTickets())
                .reservedTickets(event.getReservedTickets())
                .soldTickets(event.getSoldTickets())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
