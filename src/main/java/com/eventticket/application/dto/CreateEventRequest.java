package com.eventticket.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Data Transfer Object for creating an event.
 * Functional Requirement #1: Event management.
 */
public record CreateEventRequest(
        @NotBlank(message = "Event name cannot be blank")
        String name,
        
        @NotBlank(message = "Event description cannot be blank")
        String description,
        
        @NotBlank(message = "Venue cannot be blank")
        String venue,
        
        @NotNull(message = "Event date cannot be null")
        @Future(message = "Event date must be in the future")
        Instant eventDate,
        
        @NotNull(message = "Total capacity cannot be null")
        @Min(value = 1, message = "Total capacity must be at least 1")
        Integer totalCapacity
) {
}
