package com.eventticket.infrastructure.scheduler;

import com.eventticket.application.usecase.ReleaseExpiredReservationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for automatic release of expired reservations.
 * Functional Requirement #6: Automatic release of expired reservations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

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
