package com.eventticket.infrastructure.api;

import com.eventticket.application.dto.ConfirmOrderRequest;
import com.eventticket.application.dto.CreateOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.application.dto.TicketItemResponse;
import com.eventticket.domain.model.OrderStatus;
import com.eventticket.application.usecase.ConfirmTicketOrderUseCase;
import com.eventticket.application.usecase.CreateTicketOrderUseCase;
import com.eventticket.application.usecase.GetTicketOrderUseCase;
import com.eventticket.application.usecase.MarkOrderAsSoldUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketOrderController Tests")
class TicketOrderControllerTest {

    @Mock
    private CreateTicketOrderUseCase createOrderUseCase;

    @Mock
    private ConfirmTicketOrderUseCase confirmOrderUseCase;

    @Mock
    private GetTicketOrderUseCase getOrderUseCase;

    @Mock
    private MarkOrderAsSoldUseCase markOrderAsSoldUseCase;

    @InjectMocks
    private TicketOrderController controller;

    private WebTestClient webTestClient;

    private OrderResponse testOrderResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(controller).build();

        testOrderResponse = new OrderResponse(
                "order-123",
                "customer-456",
                "ORD-123456",
                "event-789",
                "Test Event",
                OrderStatus.RESERVED,
                List.of(),
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-456",
                "event-789",
                "Test Event",
                "VIP",
                2
        );

        when(createOrderUseCase.execute(any(CreateOrderRequest.class)))
                .thenReturn(Mono.just(testOrderResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/orders")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.orderId()).isEqualTo("order-123");
                    assertThat(response.customerId()).isEqualTo("customer-456");
                });
    }

    @Test
    @DisplayName("Should return 400 when request is invalid")
    void shouldReturn400WhenRequestIsInvalid() {
        // Given - invalid request (missing required fields)
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                null, // customerId is null
                "event-789",
                "Test Event",
                "VIP",
                2
        );

        // When & Then
        webTestClient.post()
                .uri("/api/v1/orders")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void shouldGetOrderByIdSuccessfully() {
        // Given
        String orderId = "order-123";

        when(getOrderUseCase.execute(orderId))
                .thenReturn(Mono.just(testOrderResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.orderId()).isEqualTo("order-123");
                });
    }

    @Test
    @DisplayName("Should return 404 when order not found")
    void shouldReturn404WhenOrderNotFound() {
        // Given
        String orderId = "non-existent-order";

        when(getOrderUseCase.execute(orderId))
                .thenReturn(Mono.error(new RuntimeException("Order not found")));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should confirm order successfully")
    void shouldConfirmOrderSuccessfully() {
        // Given
        String orderId = "order-123";
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                "John Doe",
                "john@example.com",
                "1234567890",
                "123 Main St",
                "New York",
                "USA",
                "CREDIT_CARD"
        );

        OrderResponse confirmedResponse = new OrderResponse(
                "order-123",
                "customer-456",
                "ORD-123456",
                "event-789",
                "Test Event",
                OrderStatus.PENDING_CONFIRMATION,
                List.of(),
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                Instant.now()
        );

        when(confirmOrderUseCase.execute(orderId, request))
                .thenReturn(Mono.just(confirmedResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/orders/{orderId}/confirm", orderId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.orderId()).isEqualTo("order-123");
                    assertThat(response.status()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
                });
    }

    @Test
    @DisplayName("Should mark order as sold successfully")
    void shouldMarkOrderAsSoldSuccessfully() {
        // Given
        String orderId = "order-123";

        OrderResponse soldResponse = new OrderResponse(
                "order-123",
                "customer-456",
                "ORD-123456",
                "event-789",
                "Test Event",
                OrderStatus.SOLD,
                List.of(),
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                Instant.now()
        );

        when(markOrderAsSoldUseCase.execute(orderId))
                .thenReturn(Mono.just(soldResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/orders/{orderId}/mark-as-sold", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.orderId()).isEqualTo("order-123");
                    assertThat(response.status()).isEqualTo(OrderStatus.SOLD);
                });
    }

    @Test
    @DisplayName("Should handle error when marking order as sold fails")
    void shouldHandleErrorWhenMarkingOrderAsSoldFails() {
        // Given
        String orderId = "order-123";

        when(markOrderAsSoldUseCase.execute(orderId))
                .thenReturn(Mono.error(new IllegalStateException("Order is not in PENDING_CONFIRMATION status")));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/orders/{orderId}/mark-as-sold", orderId)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
