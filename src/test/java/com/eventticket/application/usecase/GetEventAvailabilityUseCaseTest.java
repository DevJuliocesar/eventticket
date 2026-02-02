package com.eventticket.application.usecase;

import com.eventticket.application.dto.EventAvailabilityResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.valueobject.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetEventAvailabilityUseCase Tests")
class GetEventAvailabilityUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private GetEventAvailabilityUseCase getEventAvailabilityUseCase;

    private Event testEvent;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        testEvent = Event.create(
                "Test Event",
                "Test Description",
                "Test Venue",
                Instant.now().plusSeconds(86400),
                1000
        );
    }

    @Test
    @DisplayName("Should return event availability successfully")
    void shouldReturnEventAvailabilitySuccessfully() {
        // Given
        // Use the actual eventId from the created event
        EventId actualEventId = testEvent.getEventId();
        when(eventRepository.findById(actualEventId))
                .thenReturn(Mono.just(testEvent));

        // When
        Mono<EventAvailabilityResponse> result = getEventAvailabilityUseCase.execute(actualEventId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.eventId()).isEqualTo(actualEventId.value());
                    assertThat(response.totalCapacity()).isEqualTo(1000);
                    assertThat(response.availableTickets()).isEqualTo(1000);
                    assertThat(response.reservedTickets()).isEqualTo(0);
                    assertThat(response.soldTickets()).isEqualTo(0);
                    assertThat(response.hasAvailability()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return error when event not found")
    void shouldReturnErrorWhenEventNotFound() {
        // Given
        when(eventRepository.findById(any(EventId.class)))
                .thenReturn(Mono.empty());

        // When
        Mono<EventAvailabilityResponse> result = getEventAvailabilityUseCase.execute(eventId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof OrderNotFoundException &&
                        throwable.getMessage().contains("Event not found"))
                .verify();
    }

    @Test
    @DisplayName("Should return correct availability after reservations")
    void shouldReturnCorrectAvailabilityAfterReservations() {
        // Given
        Event eventWithReservations = testEvent.reserveTickets(50);
        when(eventRepository.findById(any(EventId.class)))
                .thenReturn(Mono.just(eventWithReservations));

        // When
        Mono<EventAvailabilityResponse> result = getEventAvailabilityUseCase.execute(eventId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.availableTickets()).isEqualTo(950);
                    assertThat(response.reservedTickets()).isEqualTo(50);
                    assertThat(response.soldTickets()).isEqualTo(0);
                })
                .verifyComplete();
    }
}
