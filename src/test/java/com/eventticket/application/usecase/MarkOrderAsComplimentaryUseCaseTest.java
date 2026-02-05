package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.model.TicketStatus;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarkOrderAsComplimentaryUseCase Tests")
class MarkOrderAsComplimentaryUseCaseTest {

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @InjectMocks
    private MarkOrderAsComplimentaryUseCase useCase;

    private EventId eventId;
    private TicketInventory testInventory;
    private List<TicketItem> testTickets;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();

        testTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );

        testInventory = TicketInventory.create(
                eventId, "VIP", "Test Event", 100, Money.of(100.0, "USD")
        );
    }

    @Test
    @DisplayName("Should mark AVAILABLE order as complimentary successfully")
    void shouldMarkAvailableOrderAsComplimentary() {
        // Given
        TicketOrder availableOrder = TicketOrder.create(
                CustomerId.of("customer-123"), eventId, "Test Event", testTickets
        );
        OrderId actualOrderId = availableOrder.getOrderId();

        List<TicketItem> complimentaryTickets = testTickets.stream()
                .map(t -> t.markAsComplimentary("VIP guest", "A-1"))
                .toList();
        TicketOrder complimentaryOrder = availableOrder.markAsComplimentary(complimentaryTickets);

        // Reserve and confirm for AVAILABLE path
        TicketInventory reservedAndConfirmed = testInventory.reserve(1).confirmReservation(1);

        when(orderRepository.findById(actualOrderId)).thenReturn(Mono.just(availableOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(complimentaryTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty());
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class), any(TicketStatus.class), any(List.class)))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class))).thenReturn(Mono.just(complimentaryOrder));
        when(orderRepository.findByEventId(eventId)).thenReturn(Flux.empty());
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(reservedAndConfirmed));

        // When
        Mono<OrderResponse> result = useCase.execute(actualOrderId.value(), "VIP guest");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(actualOrderId.value());
                    assertThat(response.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
                    assertThat(response.tickets()).hasSize(1);
                    assertThat(response.tickets().get(0).seatNumber()).isEqualTo("A-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should mark RESERVED order as complimentary successfully")
    void shouldMarkReservedOrderAsComplimentary() {
        // Given
        List<TicketItem> reservedTickets = testTickets.stream()
                .map(t -> t.reserve("system:order-processed"))
                .toList();
        TicketOrder reservedOrder = TicketOrder.create(
                CustomerId.of("customer-123"), eventId, "Test Event", reservedTickets
        ).reserve();
        OrderId actualOrderId = reservedOrder.getOrderId();

        List<TicketItem> complimentaryTickets = reservedTickets.stream()
                .map(t -> t.markAsComplimentary("promotional", "A-1"))
                .toList();
        TicketOrder complimentaryOrder = reservedOrder.markAsComplimentary(complimentaryTickets);

        TicketInventory reservedInventory = testInventory.reserve(1);
        TicketInventory confirmedInventory = reservedInventory.confirmReservation(1);

        when(orderRepository.findById(actualOrderId)).thenReturn(Mono.just(reservedOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(reservedTickets))
                .thenReturn(Flux.fromIterable(complimentaryTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty());
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class), any(TicketStatus.class), any(List.class)))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class))).thenReturn(Mono.just(complimentaryOrder));
        when(orderRepository.findByEventId(eventId)).thenReturn(Flux.empty());
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(reservedInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(confirmedInventory));

        // When
        Mono<OrderResponse> result = useCase.execute(actualOrderId.value(), "promotional");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(actualOrderId.value());
                    assertThat(response.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail when order not found")
    void shouldFailWhenOrderNotFound() {
        // Given
        OrderId testOrderId = OrderId.generate();
        when(orderRepository.findById(testOrderId)).thenReturn(Mono.empty());

        // When
        Mono<OrderResponse> result = useCase.execute(testOrderId.value(), "VIP guest");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    @DisplayName("Should fail when order is already SOLD")
    void shouldFailWhenOrderIsAlreadySold() {
        // Given
        List<TicketItem> soldTickets = testTickets.stream()
                .map(t -> t.reserve("user").confirmPayment("user").markAsSold("user", "A-1"))
                .toList();
        TicketOrder soldOrder = TicketOrder.create(
                CustomerId.of("customer-123"), eventId, "Test Event", testTickets
        ).reserve().confirm().markAsSold(soldTickets);
        OrderId actualOrderId = soldOrder.getOrderId();

        when(orderRepository.findById(actualOrderId)).thenReturn(Mono.just(soldOrder));

        // When
        Mono<OrderResponse> result = useCase.execute(actualOrderId.value(), "reason");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("cannot be marked as complimentary"))
                .verify();
    }

    @Test
    @DisplayName("Should fail when no tickets found for order")
    void shouldFailWhenNoTicketsFound() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"), eventId, "Test Event", testTickets
        );
        OrderId actualOrderId = order.getOrderId();

        when(orderRepository.findById(actualOrderId)).thenReturn(Mono.just(order));
        when(ticketItemRepository.findByOrderId(actualOrderId)).thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = useCase.execute(actualOrderId.value(), "reason");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("No tickets found"))
                .verify();
    }

    @Test
    @DisplayName("Should retry when seat assignment fails")
    void shouldRetryWhenSeatAssignmentFails() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"), eventId, "Test Event", testTickets
        );
        OrderId actualOrderId = order.getOrderId();

        List<TicketItem> complimentaryTickets = testTickets.stream()
                .map(t -> t.markAsComplimentary("promo", "A-1"))
                .toList();
        TicketOrder complimentaryOrder = order.markAsComplimentary(complimentaryTickets);
        TicketInventory reservedAndConfirmed = testInventory.reserve(1).confirmReservation(1);

        when(orderRepository.findById(actualOrderId)).thenReturn(Mono.just(order));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(complimentaryTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty());
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class), any(TicketStatus.class), any(List.class)))
                .thenReturn(Mono.error(new IllegalStateException("Transaction failed")))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class))).thenReturn(Mono.just(complimentaryOrder));
        when(orderRepository.findByEventId(eventId)).thenReturn(Flux.empty());
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(reservedAndConfirmed));

        // When
        Mono<OrderResponse> result = useCase.execute(actualOrderId.value(), "promo");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail after max retries for seat assignment")
    void shouldFailAfterMaxRetries() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"), eventId, "Test Event", testTickets
        );
        OrderId actualOrderId = order.getOrderId();

        when(orderRepository.findById(actualOrderId)).thenReturn(Mono.just(order));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty());
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class), any(TicketStatus.class), any(List.class)))
                .thenReturn(Mono.error(new IllegalStateException("Transaction failed")));
        when(orderRepository.findByEventId(eventId)).thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = useCase.execute(actualOrderId.value(), "reason");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("Failed to assign unique seats"))
                .verify();
    }
}
