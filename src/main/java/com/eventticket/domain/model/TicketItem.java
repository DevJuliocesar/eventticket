package com.eventticket.domain.model;

import com.eventticket.domain.exception.InvalidTicketStateTransitionException;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
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
    private final OrderId orderId;
    private final ReservationId reservationId;
    private final String ticketType;
    private final String seatNumber;
    private final Money price;
    private final TicketStatus status;
    private final Instant statusChangedAt;
    private final String statusChangedBy;

    private TicketItem(
            TicketId ticketId,
            OrderId orderId,
            ReservationId reservationId,
            String ticketType,
            String seatNumber,
            Money price,
            TicketStatus status,
            Instant statusChangedAt,
            String statusChangedBy
    ) {
        this.ticketId = Objects.requireNonNull(ticketId);
        this.orderId = orderId; // Can be null for tickets not yet associated with an order
        this.reservationId = reservationId; // Can be null for tickets not yet associated with a reservation
        this.ticketType = Objects.requireNonNull(ticketType);
        this.seatNumber = seatNumber;
        this.price = Objects.requireNonNull(price);
        this.status = Objects.requireNonNull(status);
        this.statusChangedAt = Objects.requireNonNull(statusChangedAt);
        this.statusChangedBy = Objects.requireNonNull(statusChangedBy);
    }

    /**
     * Creates a new ticket item with AVAILABLE status.
     * Seat number is null initially - will be assigned when ticket is SOLD or COMPLIMENTARY.
     */
    public static TicketItem create(String ticketType, Money price) {
        return new TicketItem(
                TicketId.generate(),
                null, // OrderId will be set when ticket is added to an order
                null, // ReservationId will be set when ticket is associated with a reservation
                ticketType,
                null, // Seat number assigned only when SOLD or COMPLIMENTARY
                price,
                TicketStatus.AVAILABLE,
                Instant.now(),
                "system"
        );
    }

    /**
     * Creates a new ticket item with orderId.
     */
    public static TicketItem create(OrderId orderId, String ticketType, Money price) {
        return new TicketItem(
                TicketId.generate(),
                orderId,
                null, // ReservationId will be set when ticket is associated with a reservation
                ticketType,
                null, // Seat number assigned only when SOLD or COMPLIMENTARY
                price,
                TicketStatus.AVAILABLE,
                Instant.now(),
                "system"
        );
    }

    /**
     * Creates a new ticket item with orderId and reservationId.
     */
    public static TicketItem create(OrderId orderId, ReservationId reservationId, String ticketType, Money price) {
        return new TicketItem(
                TicketId.generate(),
                orderId,
                reservationId,
                ticketType,
                null, // Seat number assigned only when SOLD or COMPLIMENTARY
                price,
                TicketStatus.AVAILABLE,
                Instant.now(),
                "system"
        );
    }

    /**
     * Creates a complimentary (free) ticket.
     * Seat number is null initially - will be assigned when ticket is marked as COMPLIMENTARY.
     */
    public static TicketItem createComplimentary(OrderId orderId, ReservationId reservationId, String ticketType, String reason) {
        return new TicketItem(
                TicketId.generate(),
                orderId,
                reservationId,
                ticketType,
                null, // Seat number assigned only when marked as COMPLIMENTARY
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
            @JsonProperty("orderId") OrderId orderId,
            @JsonProperty("reservationId") ReservationId reservationId,
            @JsonProperty("ticketType") String ticketType,
            @JsonProperty("seatNumber") String seatNumber,
            @JsonProperty("price") Money price,
            @JsonProperty("status") TicketStatus status,
            @JsonProperty("statusChangedAt") Instant statusChangedAt,
            @JsonProperty("statusChangedBy") String statusChangedBy
    ) {
        return new TicketItem(
                ticketId,
                orderId,
                reservationId,
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
        return new TicketItem(ticketId, orderId, reservationId, ticketType, seatNumber, price,
                TicketStatus.RESERVED, Instant.now(), userId);
    }

    /**
     * Transitions ticket to PENDING_CONFIRMATION status.
     * Business Rule: Only RESERVED tickets can move to pending confirmation.
     */
    public TicketItem confirmPayment(String userId) {
        validateTransition(TicketStatus.PENDING_CONFIRMATION);
        return new TicketItem(ticketId, orderId, reservationId, ticketType, seatNumber, price,
                TicketStatus.PENDING_CONFIRMATION, Instant.now(), userId);
    }

    /**
     * Transitions ticket to SOLD status (final).
     * Business Rule: Only PENDING_CONFIRMATION tickets can be sold.
     * This is a FINAL and IRREVERSIBLE state.
     * Assigns a seat number when marking as sold.
     */
    public TicketItem markAsSold(String userId, String assignedSeatNumber) {
        validateTransition(TicketStatus.SOLD);
        if (assignedSeatNumber == null || assignedSeatNumber.isBlank()) {
            throw new IllegalArgumentException("Seat number must be provided when marking ticket as sold");
        }
        return new TicketItem(ticketId, orderId, reservationId, ticketType, assignedSeatNumber, price,
                TicketStatus.SOLD, Instant.now(), userId);
    }

    /**
     * Returns ticket to AVAILABLE status.
     * Business Rule: Can happen when reservation expires or payment fails.
     */
    public TicketItem returnToAvailable(String reason) {
        validateTransition(TicketStatus.AVAILABLE);
        return new TicketItem(ticketId, orderId, reservationId, ticketType, seatNumber, price,
                TicketStatus.AVAILABLE, Instant.now(), "system:" + reason);
    }

    /**
     * Marks ticket as complimentary (final).
     * Business Rule: Can be set from any non-final state.
     * Assigns a seat number when marking as complimentary.
     */
    public TicketItem markAsComplimentary(String reason, String assignedSeatNumber) {
        validateTransition(TicketStatus.COMPLIMENTARY);
        if (assignedSeatNumber == null || assignedSeatNumber.isBlank()) {
            throw new IllegalArgumentException("Seat number must be provided when marking ticket as complimentary");
        }
        return new TicketItem(ticketId, orderId, reservationId, ticketType, assignedSeatNumber, Money.zero(),
                TicketStatus.COMPLIMENTARY, Instant.now(), "system:" + reason);
    }

    /**
     * Associates ticket with an order.
     */
    public TicketItem withOrderId(OrderId orderId) {
        return new TicketItem(ticketId, orderId, reservationId, ticketType, seatNumber, price,
                status, statusChangedAt, statusChangedBy);
    }

    /**
     * Associates ticket with a reservation.
     */
    public TicketItem withReservationId(ReservationId reservationId) {
        return new TicketItem(ticketId, orderId, reservationId, ticketType, seatNumber, price,
                status, statusChangedAt, statusChangedBy);
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
    public OrderId getOrderId() { return orderId; }
    public ReservationId getReservationId() { return reservationId; }
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
