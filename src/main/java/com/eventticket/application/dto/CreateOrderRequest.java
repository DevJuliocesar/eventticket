package com.eventticket.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for creating a ticket order.
 * Immutable record following Java 21+ best practices.
 */
public record CreateOrderRequest(
        @NotBlank(message = "Customer ID cannot be blank")
        String customerId,
        
        @NotBlank(message = "Event ID cannot be blank")
        String eventId,
        
        @NotBlank(message = "Event name cannot be blank")
        String eventName,
        
        @NotBlank(message = "Ticket type cannot be blank")
        String ticketType,
        
        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
