package com.eventticket.domain.model;

import com.eventticket.domain.exception.InsufficientInventoryException;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketInventory Domain Model Tests")
class TicketInventoryTest {

    @Test
    @DisplayName("Should create inventory with all tickets available")
    void shouldCreateInventoryWithAllTicketsAvailable() {
        // When
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );

        // Then
        assertThat(inventory.getTotalQuantity()).isEqualTo(100);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getSoldQuantity()).isEqualTo(0);
        assertThat(inventory.getPrice().getAmount()).isEqualByComparingTo(new java.math.BigDecimal("150.00"));
        assertThat(inventory.getVersion()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reserve tickets from inventory")
    void shouldReserveTicketsFromInventory() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );

        // When
        TicketInventory reserved = inventory.reserve(10);

        // Then
        assertThat(reserved.getAvailableQuantity()).isEqualTo(90);
        assertThat(reserved.getReservedQuantity()).isEqualTo(10);
        assertThat(reserved.getSoldQuantity()).isEqualTo(0);
        assertThat(reserved.getVersion()).isEqualTo(inventory.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when reserving more than available")
    void shouldThrowExceptionWhenReservingMoreThanAvailable() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );

        // When & Then
        assertThatThrownBy(() -> inventory.reserve(101))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Not enough tickets available");
    }

    @Test
    @DisplayName("Should confirm reservation")
    void shouldConfirmReservation() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        ).reserve(10);

        // When
        TicketInventory confirmed = inventory.confirmReservation(10);

        // Then
        assertThat(confirmed.getAvailableQuantity()).isEqualTo(90);
        assertThat(confirmed.getReservedQuantity()).isEqualTo(0);
        assertThat(confirmed.getSoldQuantity()).isEqualTo(10);
        assertThat(confirmed.getVersion()).isEqualTo(inventory.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when confirming more than reserved")
    void shouldThrowExceptionWhenConfirmingMoreThanReserved() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        ).reserve(10);

        // When & Then
        assertThatThrownBy(() -> inventory.confirmReservation(11))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough reserved tickets");
    }

    @Test
    @DisplayName("Should release reserved tickets")
    void shouldReleaseReservedTickets() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        ).reserve(10);

        // When
        TicketInventory released = inventory.releaseReservation(10);

        // Then
        assertThat(released.getAvailableQuantity()).isEqualTo(100);
        assertThat(released.getReservedQuantity()).isEqualTo(0);
        assertThat(released.getSoldQuantity()).isEqualTo(0);
        assertThat(released.getVersion()).isEqualTo(inventory.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when releasing more than reserved")
    void shouldThrowExceptionWhenReleasingMoreThanReserved() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        ).reserve(10);

        // When & Then
        assertThatThrownBy(() -> inventory.releaseReservation(11))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough reserved tickets");
    }

    @Test
    @DisplayName("Should check if quantity is available")
    void shouldCheckIfQuantityIsAvailable() {
        // Given
        TicketInventory inventory = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );

        // When & Then
        assertThat(inventory.isAvailable(50)).isTrue();
        assertThat(inventory.isAvailable(100)).isTrue();
        assertThat(inventory.isAvailable(101)).isFalse();
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        TicketInventory original = TicketInventory.create(
                EventId.generate(),
                "VIP",
                "Test Event",
                100,
                Money.of(150.0, "USD")
        );

        // When
        TicketInventory reserved = original.reserve(10);

        // Then
        assertThat(original.getAvailableQuantity()).isEqualTo(100);
        assertThat(reserved.getAvailableQuantity()).isEqualTo(90);
        assertThat(original).isNotSameAs(reserved);
    }
}
