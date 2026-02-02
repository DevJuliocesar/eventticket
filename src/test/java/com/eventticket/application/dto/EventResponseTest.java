package com.eventticket.application.dto;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import com.eventticket.domain.valueobject.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventResponse DTO Tests")
class EventResponseTest {

    @Test
    @DisplayName("Should convert domain Event to EventResponse")
    void shouldConvertDomainEventToEventResponse() {
        // Given
        Event event = Event.create(
                "Concert",
                "Music concert",
                "Stadium",
                Instant.now().plusSeconds(86400),
                1000
        );

        // When
        EventResponse response = EventResponse.fromDomain(event);

        // Then
        assertThat(response.eventId()).isEqualTo(event.getEventId().value());
        assertThat(response.name()).isEqualTo(event.getName());
        assertThat(response.description()).isEqualTo(event.getDescription());
        assertThat(response.venue()).isEqualTo(event.getVenue());
        assertThat(response.eventDate()).isEqualTo(event.getEventDate());
        assertThat(response.totalCapacity()).isEqualTo(event.getTotalCapacity());
        assertThat(response.availableTickets()).isEqualTo(event.getAvailableTickets());
        assertThat(response.reservedTickets()).isEqualTo(event.getReservedTickets());
        assertThat(response.soldTickets()).isEqualTo(event.getSoldTickets());
        assertThat(response.status()).isEqualTo(event.getStatus());
        assertThat(response.createdAt()).isEqualTo(event.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(event.getUpdatedAt());
    }

    @Test
    @DisplayName("Should convert event with reserved tickets")
    void shouldConvertEventWithReservedTickets() {
        // Given
        Event event = Event.create(
                "Concert",
                "Music concert",
                "Stadium",
                Instant.now().plusSeconds(86400),
                1000
        );
        Event reserved = event.reserveTickets(100);

        // When
        EventResponse response = EventResponse.fromDomain(reserved);

        // Then
        assertThat(response.availableTickets()).isEqualTo(900);
        assertThat(response.reservedTickets()).isEqualTo(100);
        assertThat(response.soldTickets()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should convert cancelled event")
    void shouldConvertCancelledEvent() {
        // Given
        Event event = Event.create(
                "Concert",
                "Music concert",
                "Stadium",
                Instant.now().plusSeconds(86400),
                1000
        );
        Event cancelled = event.cancel();

        // When
        EventResponse response = EventResponse.fromDomain(cancelled);

        // Then
        assertThat(response.status()).isEqualTo(EventStatus.CANCELLED);
    }
}
