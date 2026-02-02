package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateInventoryRequest;
import com.eventticket.domain.model.Event;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateInventoryUseCase Tests")
class CreateInventoryUseCaseTest {

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CreateInventoryUseCase createInventoryUseCase;

    private CreateInventoryRequest request;
    private Event testEvent;
    private TicketInventory savedInventory;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        request = new CreateInventoryRequest(
                eventId.value(),
                "VIP",
                100,
                new java.math.BigDecimal("150.0"),
                "USD"
        );

        testEvent = Event.create(
                "Test Event",
                "Test Description",
                "Test Venue",
                java.time.Instant.now().plusSeconds(86400),
                1000
        );

        savedInventory = TicketInventory.create(
                eventId,
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );
    }

    @Test
    @DisplayName("Should create inventory successfully")
    void shouldCreateInventorySuccessfully() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.empty());
        when(inventoryRepository.save(any(TicketInventory.class)))
                .thenReturn(Mono.just(savedInventory));

        // When
        Mono<TicketInventory> result = createInventoryUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory.getEventId()).isEqualTo(eventId);
                    assertThat(inventory.getTicketType()).isEqualTo("VIP");
                    assertThat(inventory.getTotalQuantity()).isEqualTo(100);
                    assertThat(inventory.getPrice().getAmount()).isEqualByComparingTo(new java.math.BigDecimal("150.0"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail when event not found")
    void shouldFailWhenEventNotFound() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.empty());

        // When
        Mono<TicketInventory> result = createInventoryUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Event not found"))
                .verify();
    }

    @Test
    @DisplayName("Should fail when inventory already exists")
    void shouldFailWhenInventoryAlreadyExists() {
        // Given
        when(eventRepository.findById(eventId))
                .thenReturn(Mono.just(testEvent));
        // When inventory exists, flatMap returns Mono.error
        // The issue is that switchIfEmpty receives the result of createInventory
        // but when flatMap returns error, the chain should propagate the error
        // However, there's a bug: switchIfEmpty is called with createInventory result
        // even when flatMap returns error. This is a bug in the use case implementation.
        // For now, we'll test that an error is thrown (could be IllegalStateException or NullPointerException)
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(savedInventory)); // Returns existing inventory

        // When
        Mono<TicketInventory> result = createInventoryUseCase.execute(request);

        // Then
        // After fixing the use case with Mono.defer, it should return IllegalStateException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage() != null &&
                        throwable.getMessage().contains("Inventory already exists"))
                .verify();
    }
}
