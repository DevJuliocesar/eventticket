package com.eventticket.domain.exception;

/**
 * Exception thrown when an order is not found.
 */
public class OrderNotFoundException extends DomainException {
    
    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
