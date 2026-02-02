package com.eventticket.application.usecase;

import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Use case for releasing expired reservations.
 * Functional Requirement #6: Automatic release of expired reservations.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class ReleaseExpiredReservationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseExpiredReservationsUseCase.class);

    private final TicketReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final TicketInventoryRepository inventoryRepository;

    public ReleaseExpiredReservationsUseCase(
            TicketReservationRepository reservationRepository,
            EventRepository eventRepository,
            TicketInventoryRepository inventoryRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.inventoryRepository = inventoryRepository;
    }

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
                    // Check if Event has enough reserved tickets to release
                    // This handles race conditions where the reservation was already confirmed
                    // or released by another process
                    if (event.getReservedTickets() >= reservation.getQuantity()) {
                        // Return tickets to available inventory
                        var updatedEvent = event.releaseReservedTickets(reservation.getQuantity());
                        return eventRepository.updateWithOptimisticLock(updatedEvent)
                                .thenReturn(true);
                    } else {
                        // Not enough reserved tickets - likely already confirmed or released
                        log.warn(
                                "Cannot release {} tickets for reservation {}: Event has only {} reserved tickets. " +
                                "Reservation may have been confirmed or already released.",
                                reservation.getQuantity(),
                                reservation.getReservationId(),
                                event.getReservedTickets()
                        );
                        return Mono.just(false);
                    }
                })
                .flatMap(eventReleased -> releaseInventory(reservation).thenReturn(eventReleased))
                .then(Mono.defer(() -> {
                    // Mark reservation as expired regardless of whether tickets were released
                    // This ensures the reservation is marked as expired even if the Event
                    // state has changed (e.g., reservation was confirmed)
                    var expiredReservation = reservation.expire();
                    return reservationRepository.save(expiredReservation);
                }))
                .doOnSuccess(released -> log.debug(
                        "Successfully processed expired reservation: {} with {} tickets",
                        released.getReservationId(), released.getQuantity()
                ))
                .onErrorResume(error -> {
                    // If releasing fails (e.g., optimistic lock conflict), still mark reservation as expired
                    log.error("Error releasing tickets for reservation: {}", reservation.getReservationId(), error);
                    var expiredReservation = reservation.expire();
                    return reservationRepository.save(expiredReservation)
                            .doOnSuccess(r -> log.info("Marked reservation {} as expired despite release error", r.getReservationId()));
                });
    }

    private Mono<Void> releaseInventory(TicketReservation reservation) {
        return inventoryRepository.findByEventIdAndTicketType(
                        reservation.getEventId(), 
                        reservation.getTicketType())
                .flatMap(inventory -> {
                    // Check if inventory has enough reserved tickets to release
                    if (inventory.getReservedQuantity() >= reservation.getQuantity()) {
                        // Release reservation: increases availableQuantity and reduces reservedQuantity
                        TicketInventory updatedInventory = inventory.releaseReservation(reservation.getQuantity());
                        return inventoryRepository.updateWithOptimisticLock(updatedInventory)
                                .doOnSuccess(inv -> log.debug(
                                        "Inventory released for expired reservation {}: {} tickets returned to available",
                                        reservation.getReservationId(), reservation.getQuantity()))
                                .then();
                    } else {
                        // Not enough reserved tickets - likely already confirmed or released
                        log.warn(
                                "Cannot release {} tickets in inventory for reservation {}: Inventory has only {} reserved tickets",
                                reservation.getQuantity(),
                                reservation.getReservationId(),
                                inventory.getReservedQuantity()
                        );
                        return Mono.empty();
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error releasing inventory for reservation {}: ticketType={}, quantity={}",
                            reservation.getReservationId(), reservation.getTicketType(), reservation.getQuantity(), error);
                    return Mono.empty(); // Continue even if inventory update fails
                });
    }
}
