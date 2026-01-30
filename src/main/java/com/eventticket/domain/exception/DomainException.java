package com.eventticket.domain.exception;

/**
 * Base exception for all domain-related exceptions.
 */
public class DomainException extends RuntimeException {
    
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
