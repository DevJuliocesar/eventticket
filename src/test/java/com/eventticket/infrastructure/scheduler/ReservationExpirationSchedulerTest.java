package com.eventticket.infrastructure.scheduler;

import com.eventticket.application.usecase.ReleaseExpiredReservationsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationExpirationScheduler Tests")
class ReservationExpirationSchedulerTest {

    @Mock
    private ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

    private ReservationExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReservationExpirationScheduler(releaseExpiredReservationsUseCase);
    }

    @Test
    @DisplayName("Should call use case when releasing expired reservations")
    void shouldCallUseCaseWhenReleasingExpiredReservations() {
        // Given
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.just(5L));

        // When
        scheduler.releaseExpiredReservations();

        // Then
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should handle successful release of expired reservations")
    void shouldHandleSuccessfulReleaseOfExpiredReservations() throws InterruptedException {
        // Given
        Long releasedCount = 3L;
        CountDownLatch latch = new CountDownLatch(1);
        
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.just(releasedCount)
                        .doOnSuccess(count -> latch.countDown()));

        // When
        scheduler.releaseExpiredReservations();

        // Then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should handle zero released reservations")
    void shouldHandleZeroReleasedReservations() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.just(0L)
                        .doOnSuccess(count -> latch.countDown()));

        // When
        scheduler.releaseExpiredReservations();

        // Then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should handle errors gracefully when releasing expired reservations")
    void shouldHandleErrorsGracefullyWhenReleasingExpiredReservations() throws InterruptedException {
        // Given
        RuntimeException error = new RuntimeException("Database connection failed");
        CountDownLatch latch = new CountDownLatch(1);
        
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.<Long>error(error)
                        .doOnError(e -> latch.countDown()));

        // When
        scheduler.releaseExpiredReservations();

        // Then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should not throw exception when use case returns error")
    void shouldNotThrowExceptionWhenUseCaseReturnsError() {
        // Given
        RuntimeException error = new RuntimeException("Processing error");
        
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.error(error));

        // When & Then - should not throw exception
        scheduler.releaseExpiredReservations();
        
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should handle multiple consecutive calls")
    void shouldHandleMultipleConsecutiveCalls() {
        // Given
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.just(2L));

        // When
        scheduler.releaseExpiredReservations();
        scheduler.releaseExpiredReservations();
        scheduler.releaseExpiredReservations();

        // Then
        verify(releaseExpiredReservationsUseCase, times(3)).execute();
    }

    @Test
    @DisplayName("Should handle empty Mono from use case")
    void shouldHandleEmptyMonoFromUseCase() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.<Long>empty()
                        .doOnTerminate(latch::countDown));

        // When
        scheduler.releaseExpiredReservations();

        // Then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should handle delayed Mono from use case")
    void shouldHandleDelayedMonoFromUseCase() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.just(1L)
                        .delayElement(java.time.Duration.ofMillis(100))
                        .doOnSuccess(count -> latch.countDown()));

        // When
        scheduler.releaseExpiredReservations();

        // Then
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should handle use case returning null Mono")
    void shouldHandleUseCaseReturningNullMono() {
        // Given
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(null);

        // When & Then - should handle null gracefully
        try {
            scheduler.releaseExpiredReservations();
        } catch (Exception e) {
            // NullPointerException is expected if Mono is null
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
        
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Should subscribe to Mono without blocking")
    void shouldSubscribeToMonoWithoutBlocking() {
        // Given
        when(releaseExpiredReservationsUseCase.execute())
                .thenReturn(Mono.just(5L));

        // When
        long startTime = System.currentTimeMillis();
        scheduler.releaseExpiredReservations();
        long endTime = System.currentTimeMillis();

        // Then - method should return immediately (non-blocking)
        assertThat(endTime - startTime).isLessThan(100); // Should be very fast
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
}
