package com.eventticket.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating ticket inventory.
 * Functional Requirement #1: Event inventory management.
 */
public record CreateInventoryRequest(
        @NotBlank(message = "Event ID cannot be blank")
        String eventId,
        
        @NotBlank(message = "Ticket type cannot be blank")
        String ticketType,
        
        @NotNull(message = "Total quantity cannot be null")
        @Min(value = 1, message = "Total quantity must be at least 1")
        Integer totalQuantity,
        
        @NotNull(message = "Price amount cannot be null")
        @Min(value = 0, message = "Price amount cannot be negative")
        BigDecimal priceAmount,
        
        @NotBlank(message = "Currency code cannot be blank")
        String currencyCode
) {
}
