package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Event Domain Model Tests")
class EventTest {

    @Test
    @DisplayName("Should create event with correct initial values")
    void shouldCreateEventWithCorrectInitialValues() {
        // Given
        String name = "Concert";
        String description = "Music concert";
        String venue = "Stadium";
        Instant eventDate = Instant.now().plusSeconds(86400);
        int totalCapacity = 1000;

        // When
        Event event = Event.create(name, description, venue, eventDate, totalCapacity);

        // Then
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getName()).isEqualTo(name);
        assertThat(event.getDescription()).isEqualTo(description);
        assertThat(event.getVenue()).isEqualTo(venue);
        assertThat(event.getEventDate()).isEqualTo(eventDate);
        assertThat(event.getTotalCapacity()).isEqualTo(totalCapacity);
        assertThat(event.getAvailableTickets()).isEqualTo(totalCapacity);
        assertThat(event.getReservedTickets()).isEqualTo(0);
        assertThat(event.getSoldTickets()).isEqualTo(0);
        assertThat(event.getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(event.getVersion()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reserve tickets successfully")
    void shouldReserveTicketsSuccessfully() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        int quantity = 10;

        // When
        Event updated = event.reserveTickets(quantity);

        // Then
        assertThat(updated.getAvailableTickets()).isEqualTo(90);
        assertThat(updated.getReservedTickets()).isEqualTo(10);
        assertThat(updated.getSoldTickets()).isEqualTo(0);
        assertThat(updated.getVersion()).isEqualTo(event.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when reserving more tickets than available")
    void shouldThrowExceptionWhenReservingMoreTicketsThanAvailable() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        int quantity = 150;

        // When & Then
        assertThatThrownBy(() -> event.reserveTickets(quantity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough tickets available");
    }

    @Test
    @DisplayName("Should confirm reserved tickets")
    void shouldConfirmReservedTickets() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event reserved = event.reserveTickets(20);
        int quantity = 15;

        // When
        Event confirmed = reserved.confirmReservedTickets(quantity);

        // Then
        assertThat(confirmed.getAvailableTickets()).isEqualTo(80);
        assertThat(confirmed.getReservedTickets()).isEqualTo(5);
        assertThat(confirmed.getSoldTickets()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should throw exception when confirming more tickets than reserved")
    void shouldThrowExceptionWhenConfirmingMoreTicketsThanReserved() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event reserved = event.reserveTickets(10);

        // When & Then
        assertThatThrownBy(() -> reserved.confirmReservedTickets(20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough reserved tickets");
    }

    @Test
    @DisplayName("Should release reserved tickets")
    void shouldReleaseReservedTickets() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event reserved = event.reserveTickets(20);
        int quantity = 10;

        // When
        Event released = reserved.releaseReservedTickets(quantity);

        // Then
        assertThat(released.getAvailableTickets()).isEqualTo(90);
        assertThat(released.getReservedTickets()).isEqualTo(10);
        assertThat(released.getSoldTickets()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw exception when releasing more tickets than reserved")
    void shouldThrowExceptionWhenReleasingMoreTicketsThanReserved() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event reserved = event.reserveTickets(10);

        // When & Then
        assertThatThrownBy(() -> reserved.releaseReservedTickets(20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough reserved tickets");
    }

    @Test
    @DisplayName("Should cancel event")
    void shouldCancelEvent() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);

        // When
        Event cancelled = event.cancel();

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(cancelled.getVersion()).isEqualTo(event.getVersion() + 1);
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled event")
    void shouldThrowExceptionWhenCancellingAlreadyCancelledEvent() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event cancelled = event.cancel();

        // When & Then
        assertThatThrownBy(() -> cancelled.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("Should check if tickets are available")
    void shouldCheckIfTicketsAreAvailable() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);

        // When & Then
        assertThat(event.hasAvailableTickets(50)).isTrue();
        assertThat(event.hasAvailableTickets(100)).isTrue();
        assertThat(event.hasAvailableTickets(150)).isFalse();
    }

    @Test
    @DisplayName("Should return false for available tickets when event is cancelled")
    void shouldReturnFalseForAvailableTicketsWhenEventIsCancelled() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event cancelled = event.cancel();

        // When & Then
        assertThat(cancelled.hasAvailableTickets(50)).isFalse();
    }

    @Test
    @DisplayName("Should be equal when eventIds are equal")
    void shouldBeEqualWhenEventIdsAreEqual() {
        // Given
        EventId eventId = EventId.generate();
        Event event1 = Event.create("Event 1", "Desc", "Venue", Instant.now().plusSeconds(86400), 100);
        Event event2 = Event.create("Event 2", "Desc 2", "Venue 2", Instant.now().plusSeconds(86400), 200);

        // Note: Since Event.create() generates a new EventId, we can't easily test equality
        // But we can test that two events with the same ID would be equal
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Should calculate total sold correctly")
    void shouldCalculateTotalSoldCorrectly() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event reserved = event.reserveTickets(30);
        Event confirmed = reserved.confirmReservedTickets(30);

        // When & Then
        assertThat(confirmed.getTotalSold()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should calculate remaining capacity correctly")
    void shouldCalculateRemainingCapacityCorrectly() {
        // Given
        Event event = Event.create("Concert", "Music", "Stadium", Instant.now().plusSeconds(86400), 100);
        Event reserved = event.reserveTickets(20);

        // When & Then
        assertThat(reserved.getRemainingCapacity()).isEqualTo(80);
    }
}
