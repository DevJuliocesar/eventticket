package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.infrastructure.config.ReservationProperties;
import com.eventticket.infrastructure.messaging.OrderMessage;
import com.eventticket.infrastructure.messaging.OrderMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CreateTicketOrderUseCase Tests")
class CreateTicketOrderUseCaseTest {

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @Mock
    private TicketReservationRepository reservationRepository;

    @Mock
    private OrderMessagePublisher orderMessagePublisher;

    @Mock
    private ReservationProperties reservationProperties;

    @InjectMocks
    private CreateTicketOrderUseCase createTicketOrderUseCase;

    private CreateOrderRequest request;
    private TicketInventory testInventory;
    private TicketOrder testOrder;
    private TicketReservation testReservation;
    private List<TicketItem> testTickets;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        request = new CreateOrderRequest(
                "customer-123",
                eventId.value(),
                "Test Event",
                "VIP",
                2
        );

        testInventory = TicketInventory.create(
                eventId,
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );

        testTickets = List.of(
                TicketItem.create("VIP", Money.of(150.0, "USD")),
                TicketItem.create("VIP", Money.of(150.0, "USD"))
        );

        testOrder = TicketOrder.create(
                CustomerId.of("customer-123"),
                eventId,
                "Test Event",
                testTickets
        );

        when(reservationProperties.getTimeoutMinutes())
                .thenReturn(5);

        testReservation = TicketReservation.create(
                testOrder.getOrderId(),
                eventId,
                "VIP",
                2,
                5
        );
    }

    @Test
    @DisplayName("Should create order and publish to SNS successfully")
    void shouldCreateOrderSuccessfully() {
        // Given
        TicketInventory reservedInventory = testInventory.reserve(2);
        List<TicketItem> ticketsWithIds = testTickets.stream()
                .map(ticket -> ticket.withOrderId(testOrder.getOrderId())
                        .withReservationId(testReservation.getReservationId()))
                .toList();

        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(reservedInventory));
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(testOrder));
        when(reservationRepository.save(any(TicketReservation.class)))
                .thenReturn(Mono.just(testReservation));
        when(ticketItemRepository.saveAll(any(List.class)))
                .thenReturn(Flux.fromIterable(ticketsWithIds));
        when(orderMessagePublisher.publishOrder(any(OrderMessage.class)))
                .thenReturn(Mono.empty());
        when(ticketItemRepository.findByOrderId(testOrder.getOrderId()))
                .thenReturn(Flux.fromIterable(ticketsWithIds));

        // When
        Mono<OrderResponse> result = createTicketOrderUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isNotNull();
                    assertThat(response.customerId()).isEqualTo("customer-123");
                    assertThat(response.eventId()).isEqualTo(eventId.value());
                    assertThat(response.tickets()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail when inventory not found")
    void shouldFailWhenInventoryNotFound() {
        // Given
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.empty());

        // When
        Mono<OrderResponse> result = createTicketOrderUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Inventory not found"))
                .verify();
    }

    @Test
    @DisplayName("Should fail when insufficient inventory")
    void shouldFailWhenInsufficientInventory() {
        // Given
        TicketInventory lowInventory = TicketInventory.create(
                eventId,
                "VIP",
                "Test Event",
                1, // Only 1 ticket available (request asks for 2)
                Money.of(150.0, "USD")
        );

        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(lowInventory));

        // When
        Mono<OrderResponse> result = createTicketOrderUseCase.execute(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Insufficient inventory"))
                .verify();
    }
}
