package com.eventticket.application.usecase;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseExpiredReservationsUseCase Tests")
class ReleaseExpiredReservationsUseCaseTest {

    @Mock
    private TicketReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @InjectMocks
    private ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

    private TicketReservation expiredReservation;
    private Event testEvent;
    private TicketInventory testInventory;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        ReservationId reservationId = ReservationId.generate();

        expiredReservation = TicketReservation.create(
                orderId,
                eventId,
                "VIP",
                10,
                0 // Expires immediately
        );

        testEvent = Event.create(
                "Test Event",
                "Test Description",
                "Test Venue",
                Instant.now().plusSeconds(86400),
                1000
        );
        testEvent = testEvent.reserveTickets(10);

        testInventory = TicketInventory.create(
                eventId,
                "VIP",
                "Test Event",
                100,
                Money.of(100.0, "USD")
        );
        testInventory = testInventory.reserve(10);
    }

    @Test
    @DisplayName("Should release expired reservations successfully")
    void shouldReleaseExpiredReservationsSuccessfully() {
        // Given
        Instant now = Instant.now();
        Event releasedEvent = testEvent.releaseReservedTickets(10);
        TicketInventory releasedInventory = testInventory.releaseReservation(10);
        TicketReservation expiredReservation = this.expiredReservation.expire();

        when(reservationRepository.findExpiredReservations(any(Instant.class)))
                .thenReturn(Flux.just(this.expiredReservation));
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(eventRepository.updateWithOptimisticLock(any(Event.class)))
                .thenReturn(Mono.just(releasedEvent));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(releasedInventory));
        when(reservationRepository.save(any(TicketReservation.class)))
                .thenReturn(Mono.just(expiredReservation));

        // When
        Mono<Long> result = releaseExpiredReservationsUseCase.execute();

        // Then
        StepVerifier.create(result)
                .assertNext(count -> {
                    assertThat(count).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return zero when no expired reservations")
    void shouldReturnZeroWhenNoExpiredReservations() {
        // Given
        Instant now = Instant.now();

        when(reservationRepository.findExpiredReservations(any(Instant.class)))
                .thenReturn(Flux.empty());

        // When
        Mono<Long> result = releaseExpiredReservationsUseCase.execute();

        // Then
        StepVerifier.create(result)
                .assertNext(count -> {
                    assertThat(count).isEqualTo(0L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should skip reservations not in ACTIVE status")
    void shouldSkipReservationsNotInActiveStatus() {
        // Given
        Instant now = Instant.now();
        TicketReservation expiredReservation = this.expiredReservation.expire();

        when(reservationRepository.findExpiredReservations(any(Instant.class)))
                .thenReturn(Flux.just(expiredReservation)); // Already expired (not ACTIVE)

        // When
        Mono<Long> result = releaseExpiredReservationsUseCase.execute();

        // Then
        StepVerifier.create(result)
                .assertNext(count -> {
                    assertThat(count).isEqualTo(0L);
                })
                .verifyComplete();
    }
}
