package com.eventticket.domain.exception;

/**
 * Exception thrown when there is insufficient inventory for a ticket purchase.
 */
public class InsufficientInventoryException extends DomainException {
    
    public InsufficientInventoryException(String message) {
        super(message);
    }

    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
