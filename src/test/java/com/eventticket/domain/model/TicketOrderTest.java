package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketOrder Domain Model Tests")
class TicketOrderTest {

    @Test
    @DisplayName("Should create order with AVAILABLE status")
    void shouldCreateOrderWithAvailableStatus() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();
        List<TicketItem> tickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );

        // When
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", tickets);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AVAILABLE);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getEventId()).isEqualTo(eventId);
        assertThat(order.getTickets()).hasSize(1);
        assertThat(order.getTotalAmount().getAmount()).isEqualByComparingTo(new java.math.BigDecimal("100.00"));
        assertThat(order.getVersion()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void shouldCalculateTotalAmountCorrectly() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();
        List<TicketItem> tickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD")),
                TicketItem.create("General", Money.of(50.0, "USD"))
        );

        // When
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", tickets);

        // Then
        assertThat(order.getTotalAmount().getAmount()).isEqualByComparingTo(new java.math.BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should reserve order from AVAILABLE status")
    void shouldReserveOrderFromAvailableStatus() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        );

        // When
        TicketOrder reserved = order.reserve();

        // Then
        assertThat(reserved.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(reserved.getVersion()).isEqualTo(order.getVersion() + 1);
        assertThat(reserved.getOrderId()).isEqualTo(order.getOrderId());
    }

    @Test
    @DisplayName("Should throw exception when reserving non-available order")
    void shouldThrowExceptionWhenReservingNonAvailableOrder() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        ).reserve();

        // When & Then
        assertThatThrownBy(() -> order.reserve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only available orders can be reserved");
    }

    @Test
    @DisplayName("Should confirm order from RESERVED status")
    void shouldConfirmOrderFromReservedStatus() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        ).reserve();

        // When
        TicketOrder confirmed = order.confirm();

        // Then
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(confirmed.getVersion()).isEqualTo(order.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when confirming non-reserved order")
    void shouldThrowExceptionWhenConfirmingNonReservedOrder() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        );

        // When & Then
        assertThatThrownBy(() -> order.confirm())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only reserved orders can be confirmed");
    }

    @Test
    @DisplayName("Should mark order as sold from PENDING_CONFIRMATION status")
    void shouldMarkOrderAsSoldFromPendingConfirmationStatus() {
        // Given
        TicketItem originalTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(originalTicket)
        ).reserve().confirm();

        // Create ticket with proper state transitions: AVAILABLE -> RESERVED -> PENDING_CONFIRMATION -> SOLD
        List<TicketItem> updatedTickets = List.of(
                originalTicket
                        .reserve("user-123")
                        .confirmPayment("user-123")
                        .markAsSold("user-123", "A-1")
        );

        // When
        TicketOrder sold = order.markAsSold(updatedTickets);

        // Then
        assertThat(sold.getStatus()).isEqualTo(OrderStatus.SOLD);
        assertThat(sold.getVersion()).isEqualTo(order.getVersion() + 1);
        assertThat(sold.getTickets()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw exception when marking non-pending order as sold")
    void shouldThrowExceptionWhenMarkingNonPendingOrderAsSold() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        ).reserve();

        List<TicketItem> updatedTickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );

        // When & Then
        assertThatThrownBy(() -> order.markAsSold(updatedTickets))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending confirmation orders can be marked as sold");
    }

    @Test
    @DisplayName("Should throw exception when marking as sold with empty tickets")
    void shouldThrowExceptionWhenMarkingAsSoldWithEmptyTickets() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        ).reserve().confirm();

        // When & Then
        assertThatThrownBy(() -> order.markAsSold(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Updated tickets list cannot be empty");
    }

    @Test
    @DisplayName("Should mark order as expired from RESERVED status")
    void shouldMarkOrderAsExpiredFromReservedStatus() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        ).reserve();

        // When
        TicketOrder expired = order.markAsExpired();

        // Then
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(expired.getVersion()).isEqualTo(order.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when marking non-reserved order as expired")
    void shouldThrowExceptionWhenMarkingNonReservedOrderAsExpired() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        );

        // When & Then
        assertThatThrownBy(() -> order.markAsExpired())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only reserved orders can expire");
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        TicketOrder original = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        );

        // When
        TicketOrder reserved = original.reserve();

        // Then
        assertThat(original.getStatus()).isEqualTo(OrderStatus.AVAILABLE);
        assertThat(reserved.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(original).isNotSameAs(reserved);
    }

    @Test
    @DisplayName("Should mark AVAILABLE order as complimentary")
    void shouldMarkAvailableOrderAsComplimentary() {
        // Given
        TicketItem originalTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(originalTicket)
        );

        List<TicketItem> complimentaryTickets = List.of(
                originalTicket.markAsComplimentary("VIP guest", "A-1")
        );

        // When
        TicketOrder complimentary = order.markAsComplimentary(complimentaryTickets);

        // Then
        assertThat(complimentary.getStatus()).isEqualTo(OrderStatus.COMPLIMENTARY);
        assertThat(complimentary.getTotalAmount().isZero()).isTrue();
        assertThat(complimentary.getVersion()).isEqualTo(order.getVersion() + 1);
        assertThat(complimentary.getTickets()).hasSize(1);
    }

    @Test
    @DisplayName("Should mark RESERVED order as complimentary")
    void shouldMarkReservedOrderAsComplimentary() {
        // Given
        TicketItem originalTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(originalTicket)
        ).reserve();

        List<TicketItem> complimentaryTickets = List.of(
                originalTicket.reserve("user-123").markAsComplimentary("promotional", "A-1")
        );

        // When
        TicketOrder complimentary = order.markAsComplimentary(complimentaryTickets);

        // Then
        assertThat(complimentary.getStatus()).isEqualTo(OrderStatus.COMPLIMENTARY);
        assertThat(complimentary.getTotalAmount().isZero()).isTrue();
    }

    @Test
    @DisplayName("Should mark PENDING_CONFIRMATION order as complimentary")
    void shouldMarkPendingConfirmationOrderAsComplimentary() {
        // Given
        TicketItem originalTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(originalTicket)
        ).reserve().confirm();

        List<TicketItem> complimentaryTickets = List.of(
                originalTicket.reserve("user").confirmPayment("user").markAsComplimentary("upgrade", "A-1")
        );

        // When
        TicketOrder complimentary = order.markAsComplimentary(complimentaryTickets);

        // Then
        assertThat(complimentary.getStatus()).isEqualTo(OrderStatus.COMPLIMENTARY);
    }

    @Test
    @DisplayName("Should throw exception when marking SOLD order as complimentary")
    void shouldThrowExceptionWhenMarkingSoldOrderAsComplimentary() {
        // Given
        TicketItem originalTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        List<TicketItem> soldTickets = List.of(
                originalTicket.reserve("u").confirmPayment("u").markAsSold("u", "A-1")
        );
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(originalTicket)
        ).reserve().confirm().markAsSold(soldTickets);

        // When & Then
        assertThatThrownBy(() -> order.markAsComplimentary(soldTickets))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot mark order as complimentary");
    }

    @Test
    @DisplayName("Should throw exception when marking as complimentary with empty tickets")
    void shouldThrowExceptionWhenMarkingAsComplimentaryWithEmptyTickets() {
        // Given
        TicketOrder order = TicketOrder.create(
                CustomerId.of("customer-123"),
                EventId.generate(),
                "Test Event",
                List.of(TicketItem.create("VIP", Money.of(100.0, "USD")))
        );

        // When & Then
        assertThatThrownBy(() -> order.markAsComplimentary(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Updated tickets list cannot be empty");
    }

    @Test
    @DisplayName("Should handle order with zero tickets")
    void shouldHandleOrderWithZeroTickets() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        EventId eventId = EventId.generate();

        // When
        TicketOrder order = TicketOrder.create(customerId, eventId, "Test Event", List.of());

        // Then
        assertThat(order.getTickets()).isEmpty();
        assertThat(order.getTotalAmount().isZero()).isTrue();
    }
}
