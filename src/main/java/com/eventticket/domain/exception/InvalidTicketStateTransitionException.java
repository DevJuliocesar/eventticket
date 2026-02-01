package com.eventticket.domain.exception;

/**
 * Exception thrown when an invalid ticket state transition is attempted.
 * Enforces business rules for ticket status changes.
 */
public class InvalidTicketStateTransitionException extends DomainException {
    
    public InvalidTicketStateTransitionException(String message) {
        super(message);
    }

    public InvalidTicketStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
