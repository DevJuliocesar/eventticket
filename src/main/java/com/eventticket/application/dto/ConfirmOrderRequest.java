package com.eventticket.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for confirming a ticket order.
 * Contains customer payment information.
 * Immutable record following Java 21+ best practices.
 */
public record ConfirmOrderRequest(
        @NotBlank(message = "Customer name cannot be blank")
        String customerName,
        
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be valid")
        String email,
        
        @NotBlank(message = "Phone number cannot be blank")
        String phoneNumber,
        
        String address,
        
        String city,
        
        String country,
        
        String paymentMethod
) {
}
