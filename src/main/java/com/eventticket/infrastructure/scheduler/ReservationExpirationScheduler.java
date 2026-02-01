package com.eventticket.infrastructure.scheduler;

import com.eventticket.application.usecase.ReleaseExpiredReservationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for automatic release of expired reservations.
 * Functional Requirement #6: Automatic release of expired reservations.
 * Using Java 25 - constructor injection without Lombok.
 */
@Component
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

    public ReservationExpirationScheduler(ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase) {
        this.releaseExpiredReservationsUseCase = releaseExpiredReservationsUseCase;
    }

    /**
     * Releases expired reservations every minute.
     * Runs periodically to identify and release reservations that exceeded
     * the 10-minute timeout without confirmation.
     */
    @Scheduled(fixedDelayString = "${application.reservation.check-interval-ms:60000}")
    public void releaseExpiredReservations() {
        log.debug("Running scheduled task to release expired reservations");
        
        releaseExpiredReservationsUseCase.execute()
                .subscribe(
                        count -> log.info("Scheduled task completed. Released {} reservations", count),
                        error -> log.error("Error in scheduled task for releasing reservations", error)
                );
    }
}
