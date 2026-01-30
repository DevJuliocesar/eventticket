package com.eventticket.application.usecase;

import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Use case for releasing expired reservations.
 * Functional Requirement #6: Automatic release of expired reservations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseExpiredReservationsUseCase {

    private final TicketReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    /**
     * Executes the release expired reservations use case.
     * This should be called periodically by a scheduled job.
     *
     * @return Number of reservations released
     */
    public Mono<Long> execute() {
        Instant now = Instant.now();
        log.info("Starting release of expired reservations at: {}", now);
        
        return reservationRepository.findExpiredReservations(now)
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .flatMap(this::releaseReservation)
                .count()
                .doOnSuccess(count -> log.info("Released {} expired reservations", count))
                .doOnError(error -> log.error("Error releasing expired reservations", error));
    }

    private Mono<TicketReservation> releaseReservation(TicketReservation reservation) {
        log.debug("Releasing expired reservation: {}", reservation.getReservationId());
        
        return eventRepository.findById(reservation.getEventId())
                .flatMap(event -> {
                    // Return tickets to available inventory
                    var updatedEvent = event.releaseReservedTickets(reservation.getQuantity());
                    return eventRepository.updateWithOptimisticLock(updatedEvent);
                })
                .then(Mono.defer(() -> {
                    // Mark reservation as expired
                    var expiredReservation = reservation.expire();
                    return reservationRepository.save(expiredReservation);
                }))
                .doOnSuccess(released -> log.debug(
                        "Successfully released reservation: {} with {} tickets",
                        released.getReservationId(), released.getQuantity()
                ));
    }
}
