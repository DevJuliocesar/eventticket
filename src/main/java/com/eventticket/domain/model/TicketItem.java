package com.eventticket.domain.model;

import com.eventticket.domain.exception.InvalidTicketStateTransitionException;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.TicketId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an individual ticket.
 * Enforces business rules for ticket state transitions.
 * Using Java 25 - immutable class with state machine behavior.
 */
public final class TicketItem {
    
    private final TicketId ticketId;
    private final String ticketType;
    private final String seatNumber;
    private final Money price;
    private final TicketStatus status;
    private final Instant statusChangedAt;
    private final String statusChangedBy;

    private TicketItem(
            TicketId ticketId,
            String ticketType,
            String seatNumber,
            Money price,
            TicketStatus status,
            Instant statusChangedAt,
            String statusChangedBy
    ) {
        this.ticketId = Objects.requireNonNull(ticketId);
        this.ticketType = Objects.requireNonNull(ticketType);
        this.seatNumber = seatNumber;
        this.price = Objects.requireNonNull(price);
        this.status = Objects.requireNonNull(status);
        this.statusChangedAt = Objects.requireNonNull(statusChangedAt);
        this.statusChangedBy = Objects.requireNonNull(statusChangedBy);
    }

    /**
     * Creates a new ticket item with AVAILABLE status.
     */
    public static TicketItem create(String ticketType, String seatNumber, Money price) {
        return new TicketItem(
                TicketId.generate(),
                ticketType,
                seatNumber,
                price,
                TicketStatus.AVAILABLE,
                Instant.now(),
                "system"
        );
    }

    /**
     * Creates a complimentary (free) ticket.
     */
    public static TicketItem createComplimentary(String ticketType, String seatNumber, String reason) {
        return new TicketItem(
                TicketId.generate(),
                ticketType,
                seatNumber,
                Money.zero(),
                TicketStatus.COMPLIMENTARY,
                Instant.now(),
                "system:" + reason
        );
    }

    /**
     * Factory method for Jackson deserialization.
     * Reconstructs TicketItem from JSON representation.
     */
    @JsonCreator
    public static TicketItem fromJson(
            @JsonProperty("ticketId") TicketId ticketId,
            @JsonProperty("ticketType") String ticketType,
            @JsonProperty("seatNumber") String seatNumber,
            @JsonProperty("price") Money price,
            @JsonProperty("status") TicketStatus status,
            @JsonProperty("statusChangedAt") Instant statusChangedAt,
            @JsonProperty("statusChangedBy") String statusChangedBy
    ) {
        return new TicketItem(
                ticketId,
                ticketType,
                seatNumber,
                price,
                status,
                statusChangedAt,
                statusChangedBy
        );
    }

    /**
     * Transitions ticket to RESERVED status.
     * Business Rule: Only AVAILABLE tickets can be reserved.
     */
    public TicketItem reserve(String userId) {
        validateTransition(TicketStatus.RESERVED);
        return new TicketItem(ticketId, ticketType, seatNumber, price,
                TicketStatus.RESERVED, Instant.now(), userId);
    }

    /**
     * Transitions ticket to PENDING_CONFIRMATION status.
     * Business Rule: Only RESERVED tickets can move to pending confirmation.
     */
    public TicketItem confirmPayment(String userId) {
        validateTransition(TicketStatus.PENDING_CONFIRMATION);
        return new TicketItem(ticketId, ticketType, seatNumber, price,
                TicketStatus.PENDING_CONFIRMATION, Instant.now(), userId);
    }

    /**
     * Transitions ticket to SOLD status (final).
     * Business Rule: Only PENDING_CONFIRMATION tickets can be sold.
     * This is a FINAL and IRREVERSIBLE state.
     */
    public TicketItem markAsSold(String userId) {
        validateTransition(TicketStatus.SOLD);
        return new TicketItem(ticketId, ticketType, seatNumber, price,
                TicketStatus.SOLD, Instant.now(), userId);
    }

    /**
     * Returns ticket to AVAILABLE status.
     * Business Rule: Can happen when reservation expires or payment fails.
     */
    public TicketItem returnToAvailable(String reason) {
        validateTransition(TicketStatus.AVAILABLE);
        return new TicketItem(ticketId, ticketType, seatNumber, price,
                TicketStatus.AVAILABLE, Instant.now(), "system:" + reason);
    }

    /**
     * Marks ticket as complimentary (final).
     * Business Rule: Can be set from any non-final state.
     */
    public TicketItem markAsComplimentary(String reason) {
        validateTransition(TicketStatus.COMPLIMENTARY);
        return new TicketItem(ticketId, ticketType, seatNumber, Money.zero(),
                TicketStatus.COMPLIMENTARY, Instant.now(), "system:" + reason);
    }

    /**
     * Validates if transition to target status is allowed.
     */
    private void validateTransition(TicketStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidTicketStateTransitionException(
                    "Invalid ticket state transition from %s to %s for ticket %s"
                            .formatted(status, targetStatus, ticketId)
            );
        }
    }

    public boolean isFinalState() { return status.isFinal(); }
    public boolean countsAsRevenue() { return status.countsAsRevenue(); }
    public boolean isAvailable() { return status == TicketStatus.AVAILABLE; }
    
    public Money getAccountingValue() {
        return status == TicketStatus.COMPLIMENTARY ? Money.zero() : price;
    }

    // Getters
    public TicketId getTicketId() { return ticketId; }
    public String getTicketType() { return ticketType; }
    public String getSeatNumber() { return seatNumber; }
    public Money getPrice() { return price; }
    public TicketStatus getStatus() { return status; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public String getStatusChangedBy() { return statusChangedBy; }

    @Override
    public boolean equals(Object o) {
        return o instanceof TicketItem other && Objects.equals(ticketId, other.ticketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId);
    }

    @Override
    public String toString() {
        return "TicketItem[ticketId=%s, type=%s, status=%s]".formatted(ticketId, ticketType, status);
    }
}
