package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.TicketId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity for auditing ticket state transitions.
 * Ensures all state changes are tracked and auditable.
 * Using Java 25 Record for immutable audit entries.
 * 
 * Business Rule: All ticket state transitions must be atomic and auditable.
 */
public record TicketStateTransitionAudit(
        TicketId ticketId,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        Instant transitionTime,
        String performedBy,
        String reason,
        boolean successful,
        String errorMessage
) {
    /**
     * Compact constructor with validation.
     */
    public TicketStateTransitionAudit {
        Objects.requireNonNull(ticketId, "ticketId cannot be null");
        Objects.requireNonNull(fromStatus, "fromStatus cannot be null");
        Objects.requireNonNull(toStatus, "toStatus cannot be null");
        Objects.requireNonNull(transitionTime, "transitionTime cannot be null");
        Objects.requireNonNull(performedBy, "performedBy cannot be null");
    }

    /**
     * Creates an audit entry for a successful transition.
     */
    public static TicketStateTransitionAudit success(
            TicketId ticketId,
            TicketStatus fromStatus,
            TicketStatus toStatus,
            String performedBy,
            String reason
    ) {
        return new TicketStateTransitionAudit(
                ticketId,
                fromStatus,
                toStatus,
                Instant.now(),
                performedBy,
                reason,
                true,
                null
        );
    }

    /**
     * Creates an audit entry for a failed transition attempt.
     */
    public static TicketStateTransitionAudit failure(
            TicketId ticketId,
            TicketStatus fromStatus,
            TicketStatus attemptedStatus,
            String performedBy,
            String errorMessage
    ) {
        return new TicketStateTransitionAudit(
                ticketId,
                fromStatus,
                attemptedStatus,
                Instant.now(),
                performedBy,
                "FAILED_ATTEMPT",
                false,
                errorMessage
        );
    }

    /**
     * Checks if this was a successful transition.
     */
    public boolean wasSuccessful() {
        return successful;
    }

    /**
     * Gets a human-readable description of the transition.
     */
    public String getDescription() {
        if (successful) {
            return "Ticket %s: %s → %s by %s (Reason: %s)"
                    .formatted(ticketId, fromStatus, toStatus, performedBy, reason);
        } else {
            return "Failed: Ticket %s: %s → %s by %s (Error: %s)"
                    .formatted(ticketId, fromStatus, toStatus, performedBy, errorMessage);
        }
    }
}
