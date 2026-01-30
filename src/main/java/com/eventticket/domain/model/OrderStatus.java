package com.eventticket.domain.model;

/**
 * Represents the status of a ticket order.
 */
public enum OrderStatus {
    AVAILABLE,              // Tickets available for purchase
    RESERVED,               // Temporarily reserved (10 min timeout)
    PENDING_CONFIRMATION,   // Awaiting payment confirmation
    CONFIRMED,              // Payment confirmed
    SOLD,                   // Successfully sold
    COMPLIMENTARY,          // Free/complimentary tickets
    CANCELLED,              // Cancelled by user or system
    EXPIRED,                // Reservation expired
    FAILED                  // Processing failed
}
