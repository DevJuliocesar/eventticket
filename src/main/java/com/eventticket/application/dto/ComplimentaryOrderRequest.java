package com.eventticket.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for marking an order as complimentary.
 * Pure Java Record - Java 25 style.
 */
public record ComplimentaryOrderRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
