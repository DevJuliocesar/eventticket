package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Domain entity representing a temporary ticket reservation.
 * Reservations expire after a certain time period.
 */
@Value
@Builder
@With
public class TicketReservation {
    
    private static final int RESERVATION_TIMEOUT_MINUTES = 15;
    
    ReservationId reservationId;
    OrderId orderId;
    EventId eventId;
    String ticketType;
    int quantity;
    ReservationStatus status;
    Instant expiresAt;
    Instant createdAt;

    /**
     * Creates a new ticket reservation.
     *
     * @param orderId Order identifier
     * @param eventId Event identifier
     * @param ticketType Type of ticket
     * @param quantity Number of tickets
     * @return A new TicketReservation instance
     */
    public static TicketReservation create(
            OrderId orderId,
            EventId eventId,
            String ticketType,
            int quantity
    ) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(RESERVATION_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        
        return TicketReservation.builder()
                .reservationId(ReservationId.generate())
                .orderId(orderId)
                .eventId(eventId)
                .ticketType(ticketType)
                .quantity(quantity)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();
    }

    /**
     * Confirms the reservation.
     *
     * @return Updated reservation with confirmed status
     */
    public TicketReservation confirm() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can be confirmed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot confirm expired reservation");
        }
        return this.withStatus(ReservationStatus.CONFIRMED);
    }

    /**
     * Releases the reservation.
     *
     * @return Updated reservation with released status
     */
    public TicketReservation release() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot release confirmed reservation");
        }
        return this.withStatus(ReservationStatus.RELEASED);
    }

    /**
     * Marks the reservation as expired.
     *
     * @return Updated reservation with expired status
     */
    public TicketReservation expire() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can expire");
        }
        return this.withStatus(ReservationStatus.EXPIRED);
    }

    /**
     * Checks if the reservation is expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the reservation is active and not expired.
     *
     * @return true if active and not expired, false otherwise
     */
    public boolean isActiveAndValid() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }
}
