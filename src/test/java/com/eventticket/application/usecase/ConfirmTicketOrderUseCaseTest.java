package com.eventticket.application.usecase;

import com.eventticket.application.dto.ConfirmOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.CustomerInfo;
import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.repository.CustomerInfoRepository;
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
@DisplayName("ConfirmTicketOrderUseCase Tests")
class ConfirmTicketOrderUseCaseTest {

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @Mock
    private CustomerInfoRepository customerInfoRepository;

    @InjectMocks
    private ConfirmTicketOrderUseCase confirmTicketOrderUseCase;

    private TicketOrder reservedOrder;
    private List<TicketItem> testTickets;
    private ConfirmOrderRequest request;
    private OrderId orderId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();

        testTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
                        .reserve("system") // Tickets must be in RESERVED state
        );

        TicketOrder order = TicketOrder.create(
                customerId,
                eventId,
                "Test Event",
                testTickets
        );
        reservedOrder = order.reserve();

        request = new ConfirmOrderRequest(
                "John Doe",
                "john@example.com",
                "1234567890",
                "123 Main St",
                "New York",
                "USA",
                "CREDIT_CARD"
        );
    }

    @Test
    @DisplayName("Should confirm order successfully")
    void shouldConfirmOrderSuccessfully() {
        // Given
        CustomerInfo savedCustomerInfo = CustomerInfo.create(
                reservedOrder.getCustomerId(),
                reservedOrder.getOrderId(),
                request.customerName(),
                request.email(),
                request.phoneNumber(),
                request.address(),
                request.city(),
                request.country(),
                request.paymentMethod()
        );

        List<TicketItem> confirmedTickets = testTickets.stream()
                .map(ticket -> ticket.confirmPayment(reservedOrder.getCustomerId().value()))
                .toList();

        TicketOrder confirmedOrder = reservedOrder.confirm();

        // Use the actual orderId from the reserved order
        OrderId actualOrderId = reservedOrder.getOrderId();
        
        // Tickets are already in RESERVED state from setUp()
        when(orderRepository.findById(actualOrderId))
                .thenReturn(Mono.just(reservedOrder));
        when(customerInfoRepository.save(any(CustomerInfo.class)))
                .thenReturn(Mono.just(savedCustomerInfo));
        when(ticketItemRepository.findByOrderId(actualOrderId))
                .thenReturn(Flux.fromIterable(testTickets)) // First call: get reserved tickets (already reserved in setUp)
                .thenReturn(Flux.fromIterable(confirmedTickets)); // Second call: get confirmed tickets for response
        when(ticketItemRepository.saveAll(any(List.class)))
                .thenReturn(Flux.fromIterable(confirmedTickets));
        when(orderRepository.save(any(TicketOrder.class)))
                .thenReturn(Mono.just(confirmedOrder));

        // When
        Mono<OrderResponse> result = confirmTicketOrderUseCase.execute(actualOrderId.value(), request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(actualOrderId.value());
                    assertThat(response.status()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
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
        Mono<OrderResponse> result = confirmTicketOrderUseCase.execute(orderId.value(), request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    @DisplayName("Should fail when order is not in RESERVED status")
    void shouldFailWhenOrderIsNotInReservedStatus() {
        // Given
        TicketOrder availableOrder = TicketOrder.create(
                reservedOrder.getCustomerId(),
                reservedOrder.getEventId(),
                "Test Event",
                testTickets
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Mono.just(availableOrder));

        // When
        Mono<OrderResponse> result = confirmTicketOrderUseCase.execute(orderId.value(), request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("Order must be in RESERVED status"))
                .verify();
    }
}
