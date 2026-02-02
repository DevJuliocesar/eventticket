package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateEventRequest;
import com.eventticket.application.dto.EventResponse;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.repository.EventRepository;
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
@DisplayName("CreateEventUseCase Tests")
class CreateEventUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CreateEventUseCase createEventUseCase;

    private CreateEventRequest request;
    private Event savedEvent;

    @BeforeEach
    void setUp() {
        Instant eventDate = Instant.now().plusSeconds(86400); // Tomorrow
        request = new CreateEventRequest(
                "Test Event",
                "Test Description",
                "Test Venue",
                eventDate,
                1000
        );

        savedEvent = Event.create(
                request.name(),
                request.description(),
                request.venue(),
                request.eventDate(),
                request.totalCapacity()
        );
    }

    @Test
    @DisplayName("Should create event successfully")
    void shouldCreateEventSuccessfully() {
        // Given
        when(eventRepository.save(any(Event.class)))
                .thenReturn(Mono.just(savedEvent));

        // When
        Mono<EventResponse> result = createEventUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.name()).isEqualTo("Test Event");
                    assertThat(response.description()).isEqualTo("Test Description");
                    assertThat(response.venue()).isEqualTo("Test Venue");
                    assertThat(response.totalCapacity()).isEqualTo(1000);
                    assertThat(response.eventId()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository fails")
    void shouldPropagateErrorWhenRepositoryFails() {
        // Given
        RuntimeException error = new RuntimeException("Database error");
        when(eventRepository.save(any(Event.class)))
                .thenReturn(Mono.error(error));

        // When
        Mono<EventResponse> result = createEventUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();
    }
}
