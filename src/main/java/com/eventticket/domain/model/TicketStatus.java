package com.eventticket.domain.model;

/**
 * Represents the status of an individual ticket.
 * 
 * Business Rules:
 * 1. Each ticket can only have one status at a time
 * 2. State transitions must be atomic and auditable
 * 3. RESERVED and PENDING_CONFIRMATION do not represent sales
 * 4. SOLD is final and irreversible
 * 5. COMPLIMENTARY is final but not accountable (not counted as revenue)
 */
public enum TicketStatus {
    
    /**
     * Ticket is available for purchase.
     * Initial state for all tickets.
     */
    AVAILABLE,
    
    /**
     * Ticket is temporarily reserved (typically 10-15 minutes).
     * Not counted as a sale yet.
     * Can transition back to AVAILABLE if reservation expires.
     */
    RESERVED,
    
    /**
     * Ticket is awaiting payment confirmation.
     * Not counted as a sale yet.
     * Payment is being processed.
     */
    PENDING_CONFIRMATION,
    
    /**
     * Ticket has been sold and payment confirmed.
     * FINAL STATE - Cannot be changed.
     * Counted in revenue reports.
     */
    SOLD,
    
    /**
     * Complimentary/free ticket (promotional, VIP, etc.).
     * FINAL STATE - Cannot be changed.
     * NOT counted in revenue reports (no monetary value).
     */
    COMPLIMENTARY;

    /**
     * Checks if this status represents a final state.
     *
     * @return true if status is final (SOLD or COMPLIMENTARY)
     */
    public boolean isFinal() {
        return this == SOLD || this == COMPLIMENTARY;
    }

    /**
     * Checks if this status represents a confirmed allocation.
     *
     * @return true if ticket is definitely allocated (SOLD or COMPLIMENTARY)
     */
    public boolean isConfirmed() {
        return this == SOLD || this == COMPLIMENTARY;
    }

    /**
     * Checks if this status counts as revenue.
     *
     * @return true only for SOLD status
     */
    public boolean countsAsRevenue() {
        return this == SOLD;
    }

    /**
     * Checks if transition to target status is valid.
     *
     * @param target Target status
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(TicketStatus target) {
        // Cannot change from final states
        if (this.isFinal()) {
            return false;
        }

        return switch (this) {
            case AVAILABLE -> target == RESERVED || target == COMPLIMENTARY;
            case RESERVED -> target == PENDING_CONFIRMATION || 
                           target == AVAILABLE ||  // Reservation expired
                           target == COMPLIMENTARY;
            case PENDING_CONFIRMATION -> target == SOLD || 
                                        target == AVAILABLE ||  // Payment failed
                                        target == COMPLIMENTARY;
            case SOLD, COMPLIMENTARY -> false;  // Final states
        };
    }

    /**
     * Gets valid next states from current status.
     *
     * @return Array of valid next states
     */
    public TicketStatus[] getValidTransitions() {
        return switch (this) {
            case AVAILABLE -> new TicketStatus[]{RESERVED, COMPLIMENTARY};
            case RESERVED -> new TicketStatus[]{PENDING_CONFIRMATION, AVAILABLE, COMPLIMENTARY};
            case PENDING_CONFIRMATION -> new TicketStatus[]{SOLD, AVAILABLE, COMPLIMENTARY};
            case SOLD, COMPLIMENTARY -> new TicketStatus[]{};  // No transitions allowed
        };
    }
}
