package com.eventticket.application.usecase;

import com.eventticket.application.dto.PagedEventResponse;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.repository.EventRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Concurrency Tests for Use Cases")
class ConcurrencyTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ListEventsUseCase listEventsUseCase;

    @Test
    @DisplayName("Should handle concurrent requests to list events")
    void shouldHandleConcurrentRequestsToListEvents() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Event testEvent = Event.create(
                "Test Event", "Description",
                "Venue", Instant.now().plusSeconds(86400), 1000
        );

        when(eventRepository.findAll(anyInt(), anyInt()))
                .thenReturn(Flux.just(testEvent));
        when(eventRepository.count())
                .thenReturn(Mono.just(1L));

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        Mono<PagedEventResponse> result = listEventsUseCase.execute(0, 10);
                        StepVerifier.create(result)
                                .assertNext(response -> {
                                    assertThat(response).isNotNull();
                                    assertThat(response.totalElements()).isGreaterThanOrEqualTo(0);
                                    successCount.incrementAndGet();
                                })
                                .verifyComplete();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        int totalRequests = numberOfThreads * requestsPerThread;
        assertThat(successCount.get()).isEqualTo(totalRequests);
        assertThat(errorCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle concurrent pagination requests correctly")
    void shouldHandleConcurrentPaginationRequests() throws InterruptedException {
        // Given
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Exception> errors = new ArrayList<>();

        Event testEvent = Event.create(
                "Test Event", "Description",
                "Venue", Instant.now().plusSeconds(86400), 1000
        );

        when(eventRepository.findAll(anyInt(), anyInt()))
                .thenReturn(Flux.just(testEvent));
        when(eventRepository.count())
                .thenReturn(Mono.just(100L));

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            final int page = i;
            executor.submit(() -> {
                try {
                    Mono<PagedEventResponse> result = listEventsUseCase.execute(page, 10);
                    StepVerifier.create(result)
                            .assertNext(response -> {
                                assertThat(response.page()).isEqualTo(page + 1);
                                assertThat(response.pageSize()).isEqualTo(10);
                            })
                            .verifyComplete();
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(errors).isEmpty();
    }
}
