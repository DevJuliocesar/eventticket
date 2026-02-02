package com.eventticket.application.dto;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderResponse DTO Tests")
class OrderResponseTest {

    @Test
    @DisplayName("Should convert domain TicketOrder to OrderResponse")
    void shouldConvertDomainTicketOrderToOrderResponse() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();
        List<TicketItem> tickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD")),
                TicketItem.create("General", Money.of(50.0, "USD"))
        );
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", tickets);

        // When
        OrderResponse response = OrderResponse.fromDomain(order);

        // Then
        assertThat(response.orderId()).isEqualTo(order.getOrderId().value());
        assertThat(response.customerId()).isEqualTo(order.getCustomerId().value());
        assertThat(response.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(response.eventId()).isEqualTo(order.getEventId().value());
        assertThat(response.eventName()).isEqualTo(order.getEventName());
        assertThat(response.status()).isEqualTo(order.getStatus());
        assertThat(response.tickets()).hasSize(2);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.createdAt()).isEqualTo(order.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(order.getUpdatedAt());
    }

    @Test
    @DisplayName("Should convert order with explicit tickets list")
    void shouldConvertOrderWithExplicitTicketsList() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();
        List<TicketItem> originalTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", originalTickets);
        
        // Simulate tickets loaded separately
        List<TicketItem> loadedTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD")),
                TicketItem.create("General", Money.of(50.0, "USD"))
        );

        // When
        OrderResponse response = OrderResponse.fromDomain(order, loadedTickets);

        // Then
        assertThat(response.tickets()).hasSize(2);
        assertThat(response.tickets().get(0).ticketType()).isEqualTo("VIP");
        assertThat(response.tickets().get(1).ticketType()).isEqualTo("General");
    }

    @Test
    @DisplayName("Should convert order with different status")
    void shouldConvertOrderWithDifferentStatus() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();
        List<TicketItem> tickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", tickets);
        TicketOrder reserved = order.reserve();

        // When
        OrderResponse response = OrderResponse.fromDomain(reserved);

        // Then
        assertThat(response.status()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    @DisplayName("Should convert order with empty tickets list")
    void shouldConvertOrderWithEmptyTicketsList() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", List.of());

        // When
        OrderResponse response = OrderResponse.fromDomain(order);

        // Then
        assertThat(response.tickets()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
