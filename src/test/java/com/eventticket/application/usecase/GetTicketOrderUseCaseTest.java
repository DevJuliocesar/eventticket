package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
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
@DisplayName("GetTicketOrderUseCase Tests")
class GetTicketOrderUseCaseTest {

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @InjectMocks
    private GetTicketOrderUseCase getTicketOrderUseCase;

    private TicketOrder testOrder;
    private List<TicketItem> testTickets;
    private OrderId orderId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();

        testTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD")),
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );

        testOrder = TicketOrder.create(
                customerId,
                eventId,
                "Test Event",
                testTickets
        );
    }

    @Test
    @DisplayName("Should return order with tickets successfully")
    void shouldReturnOrderWithTicketsSuccessfully() {
        // Given
        when(orderRepository.findById(orderId))
                .thenReturn(Mono.just(testOrder));
        when(ticketItemRepository.findByOrderId(orderId))
                .thenReturn(Flux.fromIterable(testTickets));

        // When
        Mono<OrderResponse> result = getTicketOrderUseCase.execute(orderId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(testOrder.getOrderId().value());
                    assertThat(response.customerId()).isEqualTo(testOrder.getCustomerId().value());
                    assertThat(response.status()).isNotNull();
                    assertThat(response.tickets()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return error when order not found")
    void shouldReturnErrorWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(orderId))
                .thenReturn(Mono.empty());

        // When
        Mono<OrderResponse> result = getTicketOrderUseCase.execute(orderId.value());

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    @DisplayName("Should return order with empty tickets list when no tickets found")
    void shouldReturnOrderWithEmptyTicketsListWhenNoTicketsFound() {
        // Given
        when(orderRepository.findById(orderId))
                .thenReturn(Mono.just(testOrder));
        when(ticketItemRepository.findByOrderId(orderId))
                .thenReturn(Flux.empty());

        // When
        Mono<OrderResponse> result = getTicketOrderUseCase.execute(orderId.value());

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(testOrder.getOrderId().value());
                    assertThat(response.tickets()).isEmpty();
                })
                .verifyComplete();
    }
}
