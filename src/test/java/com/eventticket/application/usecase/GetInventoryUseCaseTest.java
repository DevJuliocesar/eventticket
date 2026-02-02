package com.eventticket.application.usecase;

import com.eventticket.application.dto.InventoryResponse;
import com.eventticket.application.dto.PagedInventoryResponse;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetInventoryUseCase Tests")
class GetInventoryUseCaseTest {

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private GetInventoryUseCase getInventoryUseCase;

    private Event testEvent;
    private TicketInventory testInventory1;
    private TicketInventory testInventory2;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        testEvent = Event.create(
                "Test Event", "Test Description",
                "Test Venue", java.time.Instant.now().plusSeconds(86400), 1000
        );

        testInventory1 = TicketInventory.create(
                eventId, "VIP", "Test Event", 100, Money.of(100.0, "USD")
        );

        testInventory2 = TicketInventory.create(
                eventId, "General", "Test Event", 200, Money.of(50.0, "USD")
        );
    }

    @Test
    @DisplayName("Should return inventory list without pagination")
    void shouldReturnInventoryListWithoutPagination() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(inventoryRepository.findByEventId(eventId))
                .thenReturn(Flux.just(testInventory1, testInventory2));

        // When
        Flux<InventoryResponse> result = getInventoryUseCase.execute(eventId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory.ticketType()).isEqualTo("VIP");
                    assertThat(inventory.eventId()).isEqualTo(eventId.value());
                })
                .assertNext(inventory -> {
                    assertThat(inventory.ticketType()).isEqualTo("General");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return paginated inventory with default values")
    void shouldReturnPaginatedInventoryWithDefaultValues() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(inventoryRepository.findByEventId(eventId, 0, 10))
                .thenReturn(Flux.just(testInventory1, testInventory2));
        when(inventoryRepository.countByEventId(eventId))
                .thenReturn(Mono.just(2L));

        // When
        Mono<PagedInventoryResponse> result = getInventoryUseCase.execute(eventId.value(), null, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.inventories()).hasSize(2);
                    assertThat(response.page()).isEqualTo(1);
                    assertThat(response.pageSize()).isEqualTo(10);
                    assertThat(response.totalElements()).isEqualTo(2);
                    assertThat(response.hasNext()).isFalse();
                    assertThat(response.hasPrevious()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return paginated inventory with custom pagination")
    void shouldReturnPaginatedInventoryWithCustomPagination() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(inventoryRepository.findByEventId(eventId, 1, 5))
                .thenReturn(Flux.just(testInventory2));
        when(inventoryRepository.countByEventId(eventId))
                .thenReturn(Mono.just(10L));

        // When
        Mono<PagedInventoryResponse> result = getInventoryUseCase.execute(eventId.value(), 1, 5);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.inventories()).hasSize(1);
                    assertThat(response.page()).isEqualTo(2);
                    assertThat(response.pageSize()).isEqualTo(5);
                    assertThat(response.totalElements()).isEqualTo(10);
                    assertThat(response.totalPages()).isEqualTo(2);
                    assertThat(response.hasNext()).isFalse();
                    assertThat(response.hasPrevious()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should limit page size to maximum")
    void shouldLimitPageSizeToMaximum() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(inventoryRepository.findByEventId(eventId, 0, 100))
                .thenReturn(Flux.just(testInventory1));
        when(inventoryRepository.countByEventId(eventId))
                .thenReturn(Mono.just(1L));

        // When
        Mono<PagedInventoryResponse> result = getInventoryUseCase.execute(eventId.value(), 0, 200);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.pageSize()).isEqualTo(100);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return error when event not found")
    void shouldReturnErrorWhenEventNotFound() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.empty());

        // When
        Mono<PagedInventoryResponse> result = getInventoryUseCase.execute(eventId.value(), 0, 10);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof IllegalArgumentException &&
                    error.getMessage().contains("Event not found")
                )
                .verify();
    }

    @Test
    @DisplayName("Should handle empty inventory list")
    void shouldHandleEmptyInventoryList() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(inventoryRepository.findByEventId(eventId, 0, 10))
                .thenReturn(Flux.empty());
        when(inventoryRepository.countByEventId(eventId))
                .thenReturn(Mono.just(0L));

        // When
        Mono<PagedInventoryResponse> result = getInventoryUseCase.execute(eventId.value(), null, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.inventories()).isEmpty();
                    assertThat(response.totalElements()).isEqualTo(0);
                })
                .verifyComplete();
    }
}
