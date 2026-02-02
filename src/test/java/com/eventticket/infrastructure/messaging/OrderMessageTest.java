package com.eventticket.infrastructure.messaging;

import com.eventticket.domain.valueobject.OrderId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderMessage Tests")
class OrderMessageTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should create OrderMessage successfully")
    void shouldCreateOrderMessageSuccessfully() {
        // When
        OrderMessage message = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
        );

        // Then
        assertThat(message.orderId()).isEqualTo("order-123");
        assertThat(message.eventId()).isEqualTo("event-456");
        assertThat(message.customerId()).isEqualTo("customer-789");
        assertThat(message.ticketType()).isEqualTo("VIP");
        assertThat(message.quantity()).isEqualTo(2);
        assertThat(message.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create OrderMessage using factory method")
    void shouldCreateOrderMessageUsingFactoryMethod() {
        // Given
        OrderId orderId = OrderId.generate();
        String eventId = "event-456";
        String customerId = "customer-789";
        String ticketType = "VIP";
        int quantity = 2;

        // When
        OrderMessage message = OrderMessage.of(orderId, eventId, customerId, ticketType, quantity);

        // Then
        assertThat(message.orderId()).isEqualTo(orderId.value());
        assertThat(message.eventId()).isEqualTo(eventId);
        assertThat(message.customerId()).isEqualTo(customerId);
        assertThat(message.ticketType()).isEqualTo(ticketType);
        assertThat(message.quantity()).isEqualTo(quantity);
        assertThat(message.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when orderId is null")
    void shouldThrowExceptionWhenOrderIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                null,
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Order ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when eventId is null")
    void shouldThrowExceptionWhenEventIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                "order-123",
                null,
                "customer-789",
                "VIP",
                2,
                Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Event ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when customerId is null")
    void shouldThrowExceptionWhenCustomerIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                "order-123",
                "event-456",
                null,
                "VIP",
                2,
                Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Customer ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when ticketType is null")
    void shouldThrowExceptionWhenTicketTypeIsNull() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                null,
                2,
                Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Ticket type cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when timestamp is null")
    void shouldThrowExceptionWhenTimestampIsNull() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Timestamp cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when quantity is zero")
    void shouldThrowExceptionWhenQuantityIsZero() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                0,
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception when quantity is negative")
    void shouldThrowExceptionWhenQuantityIsNegative() {
        // When & Then
        assertThatThrownBy(() -> new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                -1,
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    @DisplayName("Should serialize to JSON correctly")
    void shouldSerializeToJsonCorrectly() throws Exception {
        // Given
        OrderMessage message = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.parse("2026-02-01T10:00:00Z")
        );

        // When
        String json = objectMapper.writeValueAsString(message);

        // Then
        assertThat(json).contains("order-123");
        assertThat(json).contains("event-456");
        assertThat(json).contains("customer-789");
        assertThat(json).contains("VIP");
        assertThat(json).contains("\"quantity\":2");
    }

    @Test
    @DisplayName("Should deserialize from JSON correctly")
    void shouldDeserializeFromJsonCorrectly() throws Exception {
        // Given
        String json = """
                {
                    "orderId": "order-123",
                    "eventId": "event-456",
                    "customerId": "customer-789",
                    "ticketType": "VIP",
                    "quantity": 2,
                    "timestamp": "2026-02-01T10:00:00Z"
                }
                """;

        // When
        OrderMessage message = objectMapper.readValue(json, OrderMessage.class);

        // Then
        assertThat(message.orderId()).isEqualTo("order-123");
        assertThat(message.eventId()).isEqualTo("event-456");
        assertThat(message.customerId()).isEqualTo("customer-789");
        assertThat(message.ticketType()).isEqualTo("VIP");
        assertThat(message.quantity()).isEqualTo(2);
        assertThat(message.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        OrderMessage message1 = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
        );

        // When - try to create another with same values
        OrderMessage message2 = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                message1.timestamp()
        );

        // Then
        assertThat(message1).isEqualTo(message2);
        assertThat(message1.hashCode()).isEqualTo(message2.hashCode());
    }
}
