package com.eventticket.infrastructure.api;

import com.eventticket.application.dto.*;
import com.eventticket.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventController WebFlux Tests")
class EventControllerTest {

    @Mock
    private CreateEventUseCase createEventUseCase;

    @Mock
    private GetEventAvailabilityUseCase getEventAvailabilityUseCase;

    @Mock
    private CreateInventoryUseCase createInventoryUseCase;

    @Mock
    private GetInventoryUseCase getInventoryUseCase;

    @Mock
    private ListEventsUseCase listEventsUseCase;

    @InjectMocks
    private EventController eventController;

    private WebTestClient webTestClient;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        webTestClient = WebTestClient
                .bindToController(eventController)
                .controllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    @DisplayName("Should list events with pagination")
    void shouldListEventsWithPagination() {
        // Given
        EventResponse event1 = new EventResponse(
                "event-1", "Event 1", "Description 1",
                "Venue 1", Instant.now(), 100, 80, 10, 10,
                com.eventticket.domain.model.EventStatus.ACTIVE,
                Instant.now(), Instant.now()
        );

        EventResponse event2 = new EventResponse(
                "event-2", "Event 2", "Description 2",
                "Venue 2", Instant.now(), 200, 150, 30, 20,
                com.eventticket.domain.model.EventStatus.ACTIVE,
                Instant.now(), Instant.now()
        );

        PagedEventResponse pagedResponse = PagedEventResponse.of(
                List.of(event1, event2), 1, 10, 2L
        );

        when(listEventsUseCase.execute(0, 10))
                .thenReturn(Mono.just(pagedResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/events?page=0&pageSize=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PagedEventResponse.class)
                .value(response -> {
                    org.assertj.core.api.Assertions.assertThat(response.events()).hasSize(2);
                    org.assertj.core.api.Assertions.assertThat(response.page()).isEqualTo(1);
                    org.assertj.core.api.Assertions.assertThat(response.pageSize()).isEqualTo(10);
                    org.assertj.core.api.Assertions.assertThat(response.totalElements()).isEqualTo(2);
                });
    }

    @Test
    @DisplayName("Should list events with default pagination")
    void shouldListEventsWithDefaultPagination() {
        // Given
        PagedEventResponse pagedResponse = PagedEventResponse.of(
                List.of(), 1, 10, 0L
        );

        when(listEventsUseCase.execute(null, null))
                .thenReturn(Mono.just(pagedResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PagedEventResponse.class)
                .value(response -> {
                    org.assertj.core.api.Assertions.assertThat(response.page()).isEqualTo(1);
                    org.assertj.core.api.Assertions.assertThat(response.pageSize()).isEqualTo(10);
                });
    }

    @Test
    @DisplayName("Should get inventory with pagination")
    void shouldGetInventoryWithPagination() {
        // Given
        String eventId = "event-123";
        InventoryResponse inventory1 = new InventoryResponse(
                eventId, "VIP", "Test Event",
                100, 80, 10, 10,
                BigDecimal.valueOf(100.0), "USD", 0
        );

        PagedInventoryResponse pagedResponse = PagedInventoryResponse.of(
                List.of(inventory1), 1, 10, 1L
        );

        when(getInventoryUseCase.execute(eventId, 0, 10))
                .thenReturn(Mono.just(pagedResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/events/{eventId}/inventories?page=0&pageSize=10", eventId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PagedInventoryResponse.class)
                .value(response -> {
                    org.assertj.core.api.Assertions.assertThat(response.inventories()).hasSize(1);
                    org.assertj.core.api.Assertions.assertThat(response.page()).isEqualTo(1);
                    org.assertj.core.api.Assertions.assertThat(response.totalElements()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Should handle error when event not found for inventory")
    void shouldHandleErrorWhenEventNotFoundForInventory() {
        // Given
        String eventId = "non-existent";
        when(getInventoryUseCase.execute(eventId, null, null))
                .thenReturn(Mono.error(new IllegalArgumentException("Event not found: " + eventId)));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/events/{eventId}/inventories", eventId)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Invalid argument");
    }

    @Test
    @DisplayName("Should create event successfully")
    void shouldCreateEventSuccessfully() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "New Event", "Description", "Venue", Instant.now().plusSeconds(86400), 1000
        );

        EventResponse response = new EventResponse(
                "event-new", "New Event", "Description",
                "Venue", request.eventDate(), 1000, 1000, 0, 0,
                com.eventticket.domain.model.EventStatus.ACTIVE,
                Instant.now(), Instant.now()
        );

        when(createEventUseCase.execute(any(CreateEventRequest.class)))
                .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/events")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EventResponse.class)
                .value(event -> {
                    org.assertj.core.api.Assertions.assertThat(event.name()).isEqualTo("New Event");
                    org.assertj.core.api.Assertions.assertThat(event.totalCapacity()).isEqualTo(1000);
                });
    }

    @Test
    @DisplayName("Should get event availability")
    void shouldGetEventAvailability() {
        // Given
        String eventId = "event-123";
        EventAvailabilityResponse response = new EventAvailabilityResponse(
                eventId, "Test Event", 100, 80, 10, 10,
                com.eventticket.domain.model.EventStatus.ACTIVE, true
        );

        when(getEventAvailabilityUseCase.execute(eventId))
                .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/events/{eventId}/availability", eventId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EventAvailabilityResponse.class)
                .value(avail -> {
                    org.assertj.core.api.Assertions.assertThat(avail.eventId()).isEqualTo(eventId);
                    org.assertj.core.api.Assertions.assertThat(avail.availableTickets()).isEqualTo(80);
                });
    }
}
