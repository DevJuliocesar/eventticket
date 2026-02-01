package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Domain entity representing a temporary ticket reservation.
 * Reservations expire after a certain time period.
 * Using Java 25 - immutable class with wither pattern.
 */
public final class TicketReservation {
    
    private static final int RESERVATION_TIMEOUT_MINUTES = 10;
    
    private final ReservationId reservationId;
    private final OrderId orderId;
    private final EventId eventId;
    private final String ticketType;
    private final int quantity;
    private final ReservationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    private TicketReservation(
            ReservationId reservationId,
            OrderId orderId,
            EventId eventId,
            String ticketType,
            int quantity,
            ReservationStatus status,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.reservationId = Objects.requireNonNull(reservationId);
        this.orderId = Objects.requireNonNull(orderId);
        this.eventId = Objects.requireNonNull(eventId);
        this.ticketType = Objects.requireNonNull(ticketType);
        this.quantity = quantity;
        this.status = Objects.requireNonNull(status);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Creates a new ticket reservation.
     */
    public static TicketReservation create(
            OrderId orderId,
            EventId eventId,
            String ticketType,
            int quantity
    ) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(RESERVATION_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        
        return new TicketReservation(
                ReservationId.generate(),
                orderId,
                eventId,
                ticketType,
                quantity,
                ReservationStatus.ACTIVE,
                expiresAt,
                now
        );
    }

    /**
     * Confirms the reservation.
     */
    public TicketReservation confirm() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can be confirmed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot confirm expired reservation");
        }
        return new TicketReservation(reservationId, orderId, eventId, ticketType,
                quantity, ReservationStatus.CONFIRMED, expiresAt, createdAt);
    }

    /**
     * Releases the reservation.
     */
    public TicketReservation release() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot release confirmed reservation");
        }
        return new TicketReservation(reservationId, orderId, eventId, ticketType,
                quantity, ReservationStatus.RELEASED, expiresAt, createdAt);
    }

    /**
     * Marks the reservation as expired.
     */
    public TicketReservation expire() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can expire");
        }
        return new TicketReservation(reservationId, orderId, eventId, ticketType,
                quantity, ReservationStatus.EXPIRED, expiresAt, createdAt);
    }

    /**
     * Checks if the reservation is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the reservation is active and not expired.
     */
    public boolean isActiveAndValid() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    // Getters
    public ReservationId getReservationId() { return reservationId; }
    public OrderId getOrderId() { return orderId; }
    public EventId getEventId() { return eventId; }
    public String getTicketType() { return ticketType; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof TicketReservation other && Objects.equals(reservationId, other.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    @Override
    public String toString() {
        return "TicketReservation[reservationId=%s, status=%s, eventId=%s]"
                .formatted(reservationId, status, eventId);
    }
}
