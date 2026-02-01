package com.eventticket.domain.exception;

/**
 * Exception thrown when an order is not found.
 * Using Java 25 - clean exception handling.
 */
public class OrderNotFoundException extends DomainException {
    
    public OrderNotFoundException(String orderId) {
        super("Order not found: %s".formatted(orderId));
    }

    public OrderNotFoundException(String orderId, Throwable cause) {
        super("Order not found: %s".formatted(orderId), cause);
    }
}
