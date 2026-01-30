package com.eventticket.domain.repository;

import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository interface for TicketReservation aggregate.
 */
public interface TicketReservationRepository {

    /**
     * Saves a ticket reservation.
     *
     * @param reservation Reservation to save
     * @return Saved reservation
     */
    Mono<TicketReservation> save(TicketReservation reservation);

    /**
     * Finds a reservation by its identifier.
     *
     * @param reservationId Reservation identifier
     * @return Reservation if found, empty Mono otherwise
     */
    Mono<TicketReservation> findById(ReservationId reservationId);

    /**
     * Finds reservations by order identifier.
     *
     * @param orderId Order identifier
     * @return Flux of reservations
     */
    Flux<TicketReservation> findByOrderId(OrderId orderId);

    /**
     * Finds reservations by status.
     *
     * @param status Reservation status
     * @return Flux of reservations
     */
    Flux<TicketReservation> findByStatus(ReservationStatus status);

    /**
     * Finds expired reservations.
     *
     * @param now Current timestamp
     * @return Flux of expired reservations
     */
    Flux<TicketReservation> findExpiredReservations(Instant now);

    /**
     * Deletes a reservation by its identifier.
     *
     * @param reservationId Reservation identifier
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteById(ReservationId reservationId);
}
