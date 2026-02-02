package com.eventticket.application.usecase;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
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
@DisplayName("ProcessTicketOrderUseCase Tests")
class ProcessTicketOrderUseCaseTest {

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @InjectMocks
    private ProcessTicketOrderUseCase processTicketOrderUseCase;

    private TicketOrder availableOrder;
    private List<TicketItem> testTickets;
    private OrderId orderId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();

        testTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );

        // Create order with specific orderId by using reflection or creating it properly
        availableOrder = TicketOrder.create(
                customerId,
                eventId,
                "Test Event",
                testTickets
        );
        // Note: OrderId is generated in create(), so we need to use the actual orderId from the created order
    }

    @Test
    @DisplayName("Should process order successfully")
    void shouldProcessOrderSuccessfully() {
        // Given
        // Use the actual orderId from the created order
        OrderId actualOrderId = availableOrder.getOrderId();
        
        List<TicketItem> reservedTickets = testTickets.stream()
                .map(ticket -> ticket.reserve("system:order-processed"))
                .toList();

        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(availableOrder));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets));
        @SuppressWarnings("unchecked")
        var anyTicketList = any(List.class);
        when(ticketItemRepository.saveAll(anyTicketList))
                .thenReturn(Flux.fromIterable(reservedTickets));
        when(orderRepository.save(any(TicketOrder.class)))
                .thenAnswer(invocation -> {
                    TicketOrder saved = invocation.getArgument(0);
                    return Mono.just(saved);
                });

        // When
        Mono<TicketOrder> result = processTicketOrderUseCase.execute(actualOrderId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isEqualTo(actualOrderId);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail when order not found")
    void shouldFailWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(orderId))
                .thenReturn(Mono.empty());

        // When
        Mono<TicketOrder> result = processTicketOrderUseCase.execute(orderId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Order not found"))
                .verify();
    }

    @Test
    @DisplayName("Should fail when order is not in AVAILABLE status")
    void shouldFailWhenOrderIsNotInAvailableStatus() {
        // Given
        TicketOrder reservedOrder = availableOrder.reserve();

        when(orderRepository.findById(orderId))
                .thenReturn(Mono.just(reservedOrder));

        // When
        Mono<TicketOrder> result = processTicketOrderUseCase.execute(orderId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("Order is not in AVAILABLE status"))
                .verify();
    }
}
