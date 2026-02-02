package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketInventory;
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
@DisplayName("MarkOrderAsSoldUseCase Tests")
class MarkOrderAsSoldUseCaseTest {

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @InjectMocks
    private MarkOrderAsSoldUseCase markOrderAsSoldUseCase;

    private TicketOrder pendingOrder;
    private List<TicketItem> testTickets;
    private TicketInventory testInventory;
    private OrderId orderId;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        eventId = EventId.generate();
        CustomerId customerId = CustomerId.of("customer-123");

        testTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
                        .reserve("user-123")
                        .confirmPayment("user-123")
        );

        TicketOrder order = TicketOrder.create(
                customerId,
                eventId,
                "Test Event",
                testTickets
        );
        pendingOrder = order.reserve().confirm();

        testInventory = TicketInventory.create(
                eventId,
                "VIP",
                "Test Event",
                100,
                Money.of(100.0, "USD")
        );
        // Reserve tickets first before confirming
        testInventory = testInventory.reserve(1);
    }

    @Test
    @DisplayName("Should mark order as sold successfully")
    void shouldMarkOrderAsSoldSuccessfully() {
        // Given
        List<String> seatNumbers = List.of("A-1");
        List<TicketItem> soldTickets = testTickets.stream()
                .map(ticket -> ticket.markAsSold("customer-123", "A-1"))
                .toList();

        TicketOrder soldOrder = pendingOrder.markAsSold(soldTickets);
        TicketInventory confirmedInventory = testInventory.confirmReservation(1);

        OrderId actualOrderId = pendingOrder.getOrderId();
        
        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(pendingOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(soldTickets)); // First call: get tickets, second call: get sold tickets for response
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty()); // No existing seats
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class)))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(soldOrder));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory)); // Already has 1 reserved ticket
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(confirmedInventory));
        // Mock findByEventId for verifySeatUniquenessAfterSave (if called)
        when(orderRepository.findByEventId(eventId))
                .thenReturn(Flux.empty()); // No other orders with same seats

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(actualOrderId.value());
                    assertThat(response.status()).isEqualTo(OrderStatus.SOLD);
                    assertThat(response.tickets()).hasSize(1);
                    assertThat(response.tickets().get(0).seatNumber()).isEqualTo("A-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail when order not found")
    void shouldFailWhenOrderNotFound() {
        // Given
        OrderId testOrderId = OrderId.generate();
        when(orderRepository.findById(testOrderId))
                .thenReturn(Mono.empty());

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(testOrderId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    @DisplayName("Should fail when order is not in PENDING_CONFIRMATION status")
    void shouldFailWhenOrderIsNotInPendingConfirmationStatus() {
        // Given
        TicketOrder availableOrder = TicketOrder.create(
                pendingOrder.getCustomerId(),
                pendingOrder.getEventId(),
                "Test Event",
                testTickets
        );
        OrderId testOrderId = availableOrder.getOrderId();

        when(orderRepository.findById(testOrderId))
                .thenReturn(Mono.just(availableOrder));

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(testOrderId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("Order must be in PENDING_CONFIRMATION status"))
                .verify();
    }

    @Test
    @DisplayName("Should fail when no tickets found for order")
    void shouldFailWhenNoTicketsFoundForOrder() {
        // Given
        OrderId actualOrderId = pendingOrder.getOrderId();
        
        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(pendingOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.empty()); // No tickets found

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("No tickets found for order"))
                .verify();
    }

    @Test
    @DisplayName("Should retry when seat assignment transaction fails")
    void shouldRetryWhenSeatAssignmentTransactionFails() {
        // Given
        List<String> seatNumbers = List.of("A-1");
        List<TicketItem> soldTickets = testTickets.stream()
                .map(ticket -> ticket.markAsSold("customer-123", "A-1"))
                .toList();
        TicketOrder soldOrder = pendingOrder.markAsSold(soldTickets);
        TicketInventory confirmedInventory = testInventory.confirmReservation(1);
        OrderId actualOrderId = pendingOrder.getOrderId();

        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(pendingOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(testTickets)) // Retry attempt
                .thenReturn(Flux.fromIterable(soldTickets)); // Final response
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty()) // First attempt: no existing seats
                .thenReturn(Flux.empty()); // Retry attempt: still no existing seats
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class)))
                .thenReturn(Mono.error(new IllegalStateException("Transaction failed"))) // First attempt fails
                .thenReturn(Mono.empty()); // Retry succeeds
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(soldOrder));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(confirmedInventory));
        when(orderRepository.findByEventId(eventId))
                .thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(actualOrderId.value());
                    assertThat(response.status()).isEqualTo(OrderStatus.SOLD);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should retry when seat assignment transaction fails multiple times")
    void shouldRetryWhenSeatAssignmentTransactionFailsMultipleTimes() {
        // Given
        OrderId actualOrderId = pendingOrder.getOrderId();
        List<String> seatNumbers = List.of("A-1");
        List<TicketItem> soldTickets = testTickets.stream()
                .map(ticket -> ticket.markAsSold("customer-123", "A-1"))
                .toList();
        TicketOrder soldOrder = pendingOrder.markAsSold(soldTickets);

        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(pendingOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(testTickets)) // Retry attempt 1
                .thenReturn(Flux.fromIterable(testTickets)) // Retry attempt 2
                .thenReturn(Flux.fromIterable(soldTickets)); // Final response
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty()); // No existing seats
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class)))
                .thenReturn(Mono.error(new IllegalStateException("Transaction failed"))) // Attempt 1 fails
                .thenReturn(Mono.error(new IllegalStateException("Transaction failed"))) // Attempt 2 fails
                .thenReturn(Mono.error(new IllegalStateException("Transaction failed"))); // Attempt 3 fails (max retries)
        when(orderRepository.findByEventId(eventId))
                .thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then - Should fail after max retries
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("Failed to assign unique seats after"))
                .verify();
    }

    @Test
    @DisplayName("Should generate multiple seat numbers correctly")
    void shouldGenerateMultipleSeatNumbersCorrectly() {
        // Given
        List<TicketItem> multipleTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD")).reserve("user-123").confirmPayment("user-123"),
                TicketItem.create("VIP", Money.of(100.0, "USD")).reserve("user-123").confirmPayment("user-123"),
                TicketItem.create("VIP", Money.of(100.0, "USD")).reserve("user-123").confirmPayment("user-123")
        );
        TicketOrder orderWithMultipleTickets = TicketOrder.create(
                pendingOrder.getCustomerId(),
                eventId,
                "Test Event",
                multipleTickets
        ).reserve().confirm();

        List<String> seatNumbers = List.of("A-1", "A-2", "A-3");
        List<TicketItem> soldTickets = new java.util.ArrayList<>();
        for (int i = 0; i < multipleTickets.size(); i++) {
            soldTickets.add(multipleTickets.get(i).markAsSold("customer-123", seatNumbers.get(i)));
        }
        TicketOrder soldOrder = orderWithMultipleTickets.markAsSold(soldTickets);
        TicketInventory inventoryWith3Reserved = testInventory.reserve(2); // Total 3 reserved
        TicketInventory confirmedInventory = inventoryWith3Reserved.confirmReservation(3);
        OrderId actualOrderId = orderWithMultipleTickets.getOrderId();

        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(orderWithMultipleTickets));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(multipleTickets))
                .thenReturn(Flux.fromIterable(soldTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty());
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class)))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(soldOrder));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(inventoryWith3Reserved));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(confirmedInventory));
        when(orderRepository.findByEventId(eventId))
                .thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.tickets()).hasSize(3);
                    assertThat(response.tickets().get(0).seatNumber()).isEqualTo("A-1");
                    assertThat(response.tickets().get(1).seatNumber()).isEqualTo("A-2");
                    assertThat(response.tickets().get(2).seatNumber()).isEqualTo("A-3");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when inventory update fails")
    void shouldPropagateErrorWhenInventoryUpdateFails() {
        // Given
        List<String> seatNumbers = List.of("A-1");
        List<TicketItem> soldTickets = testTickets.stream()
                .map(ticket -> ticket.markAsSold("customer-123", "A-1"))
                .toList();
        TicketOrder soldOrder = pendingOrder.markAsSold(soldTickets);
        OrderId actualOrderId = pendingOrder.getOrderId();

        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(pendingOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(soldTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.empty());
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class)))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(soldOrder));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.error(new RuntimeException("Database error")));
        when(orderRepository.findByEventId(eventId))
                .thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then - Error should propagate (inventory update failure causes overall failure)
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Database error"))
                .verify();
    }

    @Test
    @DisplayName("Should skip occupied seats when generating seat numbers")
    void shouldSkipOccupiedSeatsWhenGeneratingSeatNumbers() {
        // Given
        List<String> seatNumbers = List.of("B-1"); // A-1 is occupied, so should use B-1
        List<TicketItem> soldTickets = testTickets.stream()
                .map(ticket -> ticket.markAsSold("customer-123", "B-1"))
                .toList();
        TicketOrder soldOrder = pendingOrder.markAsSold(soldTickets);
        TicketInventory confirmedInventory = testInventory.confirmReservation(1);
        OrderId actualOrderId = pendingOrder.getOrderId();

        // Create an occupied ticket with seat A-1 (must be in correct state)
        TicketItem occupiedTicket = TicketItem.create("VIP", Money.of(100.0, "USD"))
                .reserve("other-customer")
                .confirmPayment("other-customer")
                .markAsSold("other-customer", "A-1");

        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(pendingOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets))
                .thenReturn(Flux.fromIterable(soldTickets));
        when(ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, "VIP"))
                .thenReturn(Flux.just(occupiedTicket)); // A-1 is occupied
        when(orderRepository.findByEventId(eventId))
                .thenReturn(Flux.just(TicketOrder.create(
                        CustomerId.of("other-customer"),
                        eventId,
                        "Other Event",
                        List.of(occupiedTicket)
                ))); // Another order with A-1
        when(ticketItemRepository.assignSeatsAtomically(any(List.class), any(EventId.class), any(String.class), any(List.class)))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(soldOrder));
        when(inventoryRepository.findByEventIdAndTicketType(eventId, "VIP"))
                .thenReturn(Mono.just(testInventory));
        when(inventoryRepository.updateWithOptimisticLock(any(TicketInventory.class)))
                .thenReturn(Mono.just(confirmedInventory));

        // When
        Mono<OrderResponse> result = markOrderAsSoldUseCase.execute(actualOrderId.value());

        // Then - Should assign B-1 instead of A-1
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.tickets().get(0).seatNumber()).isEqualTo("B-1");
                })
                .verifyComplete();
    }
}
