package com.eventticket.domain.model;

import com.eventticket.domain.exception.InvalidTicketStateTransitionException;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketItem Domain Model Tests")
class TicketItemTest {

    @Test
    @DisplayName("Should create ticket item with AVAILABLE status")
    void shouldCreateTicketItemWithAvailableStatus() {
        // When
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"));

        // Then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(ticket.getTicketType()).isEqualTo("VIP");
        assertThat(ticket.getPrice().getAmount()).isEqualByComparingTo(new java.math.BigDecimal("100.00"));
        assertThat(ticket.isAvailable()).isTrue();
        assertThat(ticket.isFinalState()).isFalse();
    }

    @Test
    @DisplayName("Should create ticket item with orderId")
    void shouldCreateTicketItemWithOrderId() {
        // Given
        OrderId orderId = OrderId.generate();

        // When
        TicketItem ticket = TicketItem.create(orderId, "VIP", Money.of(100.0, "USD"));

        // Then
        assertThat(ticket.getOrderId()).isEqualTo(orderId);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should create ticket item with orderId and reservationId")
    void shouldCreateTicketItemWithOrderIdAndReservationId() {
        // Given
        OrderId orderId = OrderId.generate();
        ReservationId reservationId = ReservationId.generate();

        // When
        TicketItem ticket = TicketItem.create(orderId, reservationId, "VIP", Money.of(100.0, "USD"));

        // Then
        assertThat(ticket.getOrderId()).isEqualTo(orderId);
        assertThat(ticket.getReservationId()).isEqualTo(reservationId);
    }

    @Test
    @DisplayName("Should create complimentary ticket")
    void shouldCreateComplimentaryTicket() {
        // Given
        OrderId orderId = OrderId.generate();
        ReservationId reservationId = ReservationId.generate();

        // When
        TicketItem ticket = TicketItem.createComplimentary(orderId, reservationId, "VIP", "Promotion");

        // Then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLIMENTARY);
        assertThat(ticket.getPrice().isZero()).isTrue();
        assertThat(ticket.isFinalState()).isTrue();
    }

    @Test
    @DisplayName("Should transition from AVAILABLE to RESERVED")
    void shouldTransitionFromAvailableToReserved() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"));

        // When
        TicketItem reserved = ticket.reserve("user-123");

        // Then
        assertThat(reserved.getStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(reserved.getStatusChangedBy()).isEqualTo("user-123");
        assertThat(reserved.getTicketId()).isEqualTo(ticket.getTicketId());
    }

    @Test
    @DisplayName("Should transition from RESERVED to PENDING_CONFIRMATION")
    void shouldTransitionFromReservedToPendingConfirmation() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"))
                .reserve("user-123");

        // When
        TicketItem pending = ticket.confirmPayment("user-123");

        // Then
        assertThat(pending.getStatus()).isEqualTo(TicketStatus.PENDING_CONFIRMATION);
    }

    @Test
    @DisplayName("Should transition from PENDING_CONFIRMATION to SOLD")
    void shouldTransitionFromPendingConfirmationToSold() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"))
                .reserve("user-123")
                .confirmPayment("user-123");

        // When
        TicketItem sold = ticket.markAsSold("user-123", "A-1");

        // Then
        assertThat(sold.getStatus()).isEqualTo(TicketStatus.SOLD);
        assertThat(sold.getSeatNumber()).isEqualTo("A-1");
        assertThat(sold.isFinalState()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when marking as sold without seat number")
    void shouldThrowExceptionWhenMarkingAsSoldWithoutSeatNumber() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"))
                .reserve("user-123")
                .confirmPayment("user-123");

        // When & Then
        assertThatThrownBy(() -> ticket.markAsSold("user-123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seat number must be provided");
    }

    @Test
    @DisplayName("Should return to AVAILABLE from RESERVED")
    void shouldReturnToAvailableFromReserved() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"))
                .reserve("user-123");

        // When
        TicketItem available = ticket.returnToAvailable("Reservation expired");

        // Then
        assertThat(available.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(available.getStatusChangedBy()).contains("system:Reservation expired");
    }

    @Test
    @DisplayName("Should throw exception when transitioning from invalid state")
    void shouldThrowExceptionWhenTransitioningFromInvalidState() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"));

        // When & Then - Cannot go directly from AVAILABLE to PENDING_CONFIRMATION
        assertThatThrownBy(() -> ticket.confirmPayment("user-123"))
                .isInstanceOf(InvalidTicketStateTransitionException.class);
    }

    @Test
    @DisplayName("Should mark as complimentary with seat number")
    void shouldMarkAsComplimentaryWithSeatNumber() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"))
                .reserve("user-123");

        // When
        TicketItem complimentary = ticket.markAsComplimentary("Promotion", "B-5");

        // Then
        assertThat(complimentary.getStatus()).isEqualTo(TicketStatus.COMPLIMENTARY);
        assertThat(complimentary.getSeatNumber()).isEqualTo("B-5");
        assertThat(complimentary.getPrice().isZero()).isTrue();
        assertThat(complimentary.isFinalState()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when marking as complimentary without seat number")
    void shouldThrowExceptionWhenMarkingAsComplimentaryWithoutSeatNumber() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"));

        // When & Then
        assertThatThrownBy(() -> ticket.markAsComplimentary("Promotion", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seat number must be provided");
    }

    @Test
    @DisplayName("Should associate ticket with orderId")
    void shouldAssociateTicketWithOrderId() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        OrderId orderId = OrderId.generate();

        // When
        TicketItem withOrder = ticket.withOrderId(orderId);

        // Then
        assertThat(withOrder.getOrderId()).isEqualTo(orderId);
        assertThat(withOrder.getTicketId()).isEqualTo(ticket.getTicketId());
    }

    @Test
    @DisplayName("Should associate ticket with reservationId")
    void shouldAssociateTicketWithReservationId() {
        // Given
        TicketItem ticket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        ReservationId reservationId = ReservationId.generate();

        // When
        TicketItem withReservation = ticket.withReservationId(reservationId);

        // Then
        assertThat(withReservation.getReservationId()).isEqualTo(reservationId);
        assertThat(withReservation.getTicketId()).isEqualTo(ticket.getTicketId());
    }

    @Test
    @DisplayName("Should return correct accounting value")
    void shouldReturnCorrectAccountingValue() {
        // Given
        TicketItem regularTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
        TicketItem complimentaryTicket = TicketItem.createComplimentary(
                OrderId.generate(),
                ReservationId.generate(),
                "VIP",
                "Promotion"
        );

        // When & Then
        assertThat(regularTicket.getAccountingValue().getAmount())
                .isEqualByComparingTo(new java.math.BigDecimal("100.00"));
        assertThat(complimentaryTicket.getAccountingValue().isZero()).isTrue();
    }

    @Test
    @DisplayName("Should check if ticket counts as revenue")
    void shouldCheckIfTicketCountsAsRevenue() {
        // Given
        TicketItem available = TicketItem.create("VIP", Money.of(100.0, "USD"));
        TicketItem reserved = available.reserve("user-123");
        TicketItem pending = reserved.confirmPayment("user-123");
        TicketItem sold = pending.markAsSold("user-123", "A-1");
        TicketItem complimentary = TicketItem.createComplimentary(
                OrderId.generate(),
                ReservationId.generate(),
                "VIP",
                "Promotion"
        );

        // When & Then
        assertThat(available.countsAsRevenue()).isFalse();
        assertThat(reserved.countsAsRevenue()).isFalse();
        assertThat(pending.countsAsRevenue()).isFalse();
        assertThat(sold.countsAsRevenue()).isTrue();
        assertThat(complimentary.countsAsRevenue()).isFalse();
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        TicketItem original = TicketItem.create("VIP", Money.of(100.0, "USD"));

        // When
        TicketItem reserved = original.reserve("user-123");

        // Then
        assertThat(original.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(reserved.getStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(original).isNotSameAs(reserved);
    }
}
