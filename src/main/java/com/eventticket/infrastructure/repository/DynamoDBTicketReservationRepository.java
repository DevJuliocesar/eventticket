package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of TicketReservationRepository.
 * TODO: Replace with DynamoDB implementation.
 */
@Repository
public class DynamoDBTicketReservationRepository implements TicketReservationRepository {

    private final ConcurrentMap<String, TicketReservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Mono<TicketReservation> save(TicketReservation reservation) {
        reservations.put(reservation.getReservationId().value(), reservation);
        return Mono.just(reservation);
    }

    @Override
    public Mono<TicketReservation> findById(ReservationId reservationId) {
        TicketReservation reservation = reservations.get(reservationId.value());
        return reservation != null ? Mono.just(reservation) : Mono.empty();
    }

    @Override
    public Flux<TicketReservation> findByOrderId(OrderId orderId) {
        return Flux.fromIterable(reservations.values())
                .filter(reservation -> reservation.getOrderId().equals(orderId));
    }

    @Override
    public Flux<TicketReservation> findByStatus(ReservationStatus status) {
        return Flux.fromIterable(reservations.values())
                .filter(reservation -> reservation.getStatus() == status);
    }

    @Override
    public Flux<TicketReservation> findExpiredReservations(Instant now) {
        return Flux.fromIterable(reservations.values())
                .filter(reservation -> reservation.isExpired() && 
                        reservation.getStatus() == ReservationStatus.ACTIVE);
    }

    @Override
    public Mono<Void> deleteById(ReservationId reservationId) {
        reservations.remove(reservationId.value());
        return Mono.empty();
    }
}
