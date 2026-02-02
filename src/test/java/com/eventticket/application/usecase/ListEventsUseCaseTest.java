package com.eventticket.application.usecase;

import com.eventticket.application.dto.EventResponse;
import com.eventticket.application.dto.PagedEventResponse;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.valueobject.EventId;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListEventsUseCase Tests")
class ListEventsUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ListEventsUseCase listEventsUseCase;

    private Event testEvent1;
    private Event testEvent2;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        testEvent1 = Event.create(
                "Concierto Rock", "Gran concierto de rock",
                "Estadio Nacional", now.plusSeconds(86400), 1000
        );

        testEvent2 = Event.create(
                "Festival Jazz", "Festival de jazz internacional",
                "Teatro Municipal", now.plusSeconds(172800), 500
        );
    }

    @Test
    @DisplayName("Should list events with default pagination")
    void shouldListEventsWithDefaultPagination() {
        // Given
        when(eventRepository.findAll(0, 10))
                .thenReturn(Flux.just(testEvent1, testEvent2));
        when(eventRepository.count())
                .thenReturn(Mono.just(2L));

        // When
        Mono<PagedEventResponse> result = listEventsUseCase.execute(null, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.events()).hasSize(2);
                    assertThat(response.page()).isEqualTo(1);
                    assertThat(response.pageSize()).isEqualTo(10);
                    assertThat(response.totalElements()).isEqualTo(2);
                    assertThat(response.totalPages()).isEqualTo(1);
                    assertThat(response.hasNext()).isFalse();
                    assertThat(response.hasPrevious()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should list events with custom pagination")
    void shouldListEventsWithCustomPagination() {
        // Given
        when(eventRepository.findAll(1, 5))
                .thenReturn(Flux.just(testEvent2));
        when(eventRepository.count())
                .thenReturn(Mono.just(10L));

        // When
        Mono<PagedEventResponse> result = listEventsUseCase.execute(1, 5);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.events()).hasSize(1);
                    assertThat(response.page()).isEqualTo(2);
                    assertThat(response.pageSize()).isEqualTo(5);
                    assertThat(response.totalElements()).isEqualTo(10);
                    assertThat(response.totalPages()).isEqualTo(2);
                    assertThat(response.hasNext()).isFalse();
                    assertThat(response.hasPrevious()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should limit page size to maximum")
    void shouldLimitPageSizeToMaximum() {
        // Given
        when(eventRepository.findAll(0, 100))
                .thenReturn(Flux.just(testEvent1));
        when(eventRepository.count())
                .thenReturn(Mono.just(1L));

        // When
        Mono<PagedEventResponse> result = listEventsUseCase.execute(0, 200);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.pageSize()).isEqualTo(100);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty results")
    void shouldHandleEmptyResults() {
        // Given
        when(eventRepository.findAll(0, 10))
                .thenReturn(Flux.empty());
        when(eventRepository.count())
                .thenReturn(Mono.just(0L));

        // When
        Mono<PagedEventResponse> result = listEventsUseCase.execute(null, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.events()).isEmpty();
                    assertThat(response.totalElements()).isEqualTo(0);
                    assertThat(response.totalPages()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle negative page as default")
    void shouldHandleNegativePageAsDefault() {
        // Given
        when(eventRepository.findAll(0, 10))
                .thenReturn(Flux.just(testEvent1));
        when(eventRepository.count())
                .thenReturn(Mono.just(1L));

        // When
        Mono<PagedEventResponse> result = listEventsUseCase.execute(-1, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.page()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle zero page size as default")
    void shouldHandleZeroPageSizeAsDefault() {
        // Given
        when(eventRepository.findAll(0, 10))
                .thenReturn(Flux.just(testEvent1));
        when(eventRepository.count())
                .thenReturn(Mono.just(1L));

        // When
        Mono<PagedEventResponse> result = listEventsUseCase.execute(0, 0);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.pageSize()).isEqualTo(10);
                })
                .verifyComplete();
    }
}
