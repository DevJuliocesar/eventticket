package com.eventticket.application.dto;

import com.eventticket.domain.model.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagedEventResponse DTO Tests")
class PagedEventResponseTest {

    @Test
    @DisplayName("Should create paginated response correctly")
    void shouldCreatePaginatedResponse() {
        // Given
        List<EventResponse> events = List.of(
                new EventResponse("id1", "Event 1", "Desc 1", "Venue 1",
                        Instant.now(), 100, 80, 10, 10, EventStatus.ACTIVE,
                        Instant.now(), Instant.now()),
                new EventResponse("id2", "Event 2", "Desc 2", "Venue 2",
                        Instant.now(), 200, 150, 30, 20, EventStatus.ACTIVE,
                        Instant.now(), Instant.now())
        );

        // When
        PagedEventResponse response = PagedEventResponse.of(events, 1, 10, 25L);

        // Then
        assertThat(response.events()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should calculate pagination flags correctly")
    void shouldCalculatePaginationFlags() {
        // Given
        List<EventResponse> events = new ArrayList<>();
        long totalElements = 50L;
        int pageSize = 10;

        // When - First page
        PagedEventResponse firstPage = PagedEventResponse.of(events, 1, pageSize, totalElements);
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.hasNext()).isTrue();

        // When - Middle page
        PagedEventResponse middlePage = PagedEventResponse.of(events, 3, pageSize, totalElements);
        assertThat(middlePage.hasPrevious()).isTrue();
        assertThat(middlePage.hasNext()).isTrue();

        // When - Last page
        PagedEventResponse lastPage = PagedEventResponse.of(events, 5, pageSize, totalElements);
        assertThat(lastPage.hasPrevious()).isTrue();
        assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty results")
    void shouldHandleEmptyResults() {
        // Given
        List<EventResponse> events = new ArrayList<>();

        // When
        PagedEventResponse response = PagedEventResponse.of(events, 1, 10, 0L);

        // Then
        assertThat(response.events()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }
}
