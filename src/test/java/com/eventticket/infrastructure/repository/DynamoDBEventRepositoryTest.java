    package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import com.eventticket.domain.valueobject.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.paginators.ScanPublisher;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDBEventRepository Tests")
class DynamoDBEventRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @InjectMocks
    private DynamoDBEventRepository eventRepository;

    private Event testEvent;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        testEvent = Event.create(
                "Test Event", "Test Description",
                "Test Venue", Instant.now().plusSeconds(86400), 1000
        );
    }

    @SuppressWarnings("unchecked")
    private ScanPublisher mockScanPublisher(ScanResponse... responses) {
        ScanPublisher scanPublisher = Mockito.mock(ScanPublisher.class);
        Publisher<ScanResponse> publisher = Flux.fromArray(responses);
        // Use doAnswer to avoid UnfinishedStubbing issues
        doAnswer(invocation -> {
            Subscriber<? super ScanResponse> subscriber = (Subscriber<? super ScanResponse>) invocation.getArgument(0);
            publisher.subscribe(subscriber);
            return null;
        }).when(scanPublisher).subscribe(any(Subscriber.class));
        return scanPublisher;
    }

    @Test
    @DisplayName("Should save event successfully")
    void shouldSaveEventSuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Event> result = eventRepository.save(testEvent);

        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event.getEventId()).isEqualTo(testEvent.getEventId());
                    assertThat(event.getName()).isEqualTo("Test Event");
                    assertThat(event.getTotalCapacity()).isEqualTo(1000);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find event by ID when exists")
    void shouldFindEventByIdWhenExists() {
        // Given
        Map<String, AttributeValue> item = createEventItem(testEvent);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(item)
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Event> result = eventRepository.findById(testEvent.getEventId());

        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event.getEventId()).isEqualTo(testEvent.getEventId());
                    assertThat(event.getName()).isEqualTo(testEvent.getName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when event not found")
    void shouldReturnEmptyWhenEventNotFound() {
        // Given
        GetItemResponse getResponse = GetItemResponse.builder()
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Event> result = eventRepository.findById(eventId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find events by status")
    void shouldFindEventsByStatus() {
        // Given
        Map<String, AttributeValue> item1 = createEventItem(testEvent);
        Map<String, AttributeValue> item2 = createEventItem(Event.create(
                "Event 2", "Description 2",
                "Venue 2", Instant.now().plusSeconds(172800), 500
        ));

        ScanResponse scanResponse = ScanResponse.builder()
                .items(item1, item2)
                .build();

        ScanPublisher scanPublisher = Mockito.mock(ScanPublisher.class);
        Publisher<ScanResponse> publisher = Flux.just(scanResponse);
        doAnswer(invocation -> {
            Subscriber<? super ScanResponse> subscriber = (Subscriber<? super ScanResponse>) invocation.getArgument(0);
            publisher.subscribe(subscriber);
            return null;
        }).when(scanPublisher).subscribe(any(Subscriber.class));
        
        when(dynamoDbClient.scanPaginator(any(ScanRequest.class)))
                .thenReturn(scanPublisher);

        // When
        Flux<Event> result = eventRepository.findByStatus(EventStatus.ACTIVE);

        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event.getStatus()).isEqualTo(EventStatus.ACTIVE);
                })
                .assertNext(event -> {
                    assertThat(event.getStatus()).isEqualTo(EventStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find all events with pagination")
    void shouldFindAllEventsWithPagination() {
        // Given
        Map<String, AttributeValue> item = createEventItem(testEvent);
        ScanResponse scanResponse = ScanResponse.builder()
                .items(item)
                .build();

        ScanPublisher scanPublisher = Mockito.mock(ScanPublisher.class);
        Publisher<ScanResponse> publisher = Flux.just(scanResponse);
        doAnswer(invocation -> {
            Subscriber<? super ScanResponse> subscriber = (Subscriber<? super ScanResponse>) invocation.getArgument(0);
            publisher.subscribe(subscriber);
            return null;
        }).when(scanPublisher).subscribe(any(Subscriber.class));
        
        when(dynamoDbClient.scanPaginator(any(ScanRequest.class)))
                .thenReturn(scanPublisher);

        // When
        Flux<Event> result = eventRepository.findAll(0, 10);

        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count all events")
    void shouldCountAllEvents() {
        // Given
        ScanResponse scanResponse = ScanResponse.builder()
                .count(5)
                .build();

        ScanPublisher scanPublisher = Mockito.mock(ScanPublisher.class);
        Publisher<ScanResponse> publisher = Flux.just(scanResponse);
        doAnswer(invocation -> {
            Subscriber<? super ScanResponse> subscriber = (Subscriber<? super ScanResponse>) invocation.getArgument(0);
            publisher.subscribe(subscriber);
            return null;
        }).when(scanPublisher).subscribe(any(Subscriber.class));
        
        when(dynamoDbClient.scanPaginator(any(ScanRequest.class)))
                .thenReturn(scanPublisher);

        // When
        Mono<Long> result = eventRepository.count();

        // Then
        StepVerifier.create(result)
                .assertNext(count -> {
                    assertThat(count).isEqualTo(5L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should update event with optimistic lock")
    void shouldUpdateEventWithOptimisticLock() {
        // Given
        Event updatedEvent = testEvent.reserveTickets(10);
        
        Map<String, AttributeValue> existingItem = createEventItem(testEvent);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(existingItem)
                .build();
        CompletableFuture<GetItemResponse> getFuture = CompletableFuture.completedFuture(getResponse);

        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> putFuture = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(getFuture);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(putFuture);

        // When
        Mono<Event> result = eventRepository.updateWithOptimisticLock(updatedEvent);

        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event.getVersion()).isEqualTo(updatedEvent.getVersion());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete event by ID")
    void shouldDeleteEventById() {
        // Given
        DeleteItemResponse deleteResponse = DeleteItemResponse.builder().build();
        CompletableFuture<DeleteItemResponse> future = CompletableFuture.completedFuture(deleteResponse);

        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Void> result = eventRepository.deleteById(eventId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find upcoming events")
    void shouldFindUpcomingEvents() {
        // Given
        Instant from = Instant.now();
        Map<String, AttributeValue> item = createEventItem(testEvent);
        ScanResponse scanResponse = ScanResponse.builder()
                .items(item)
                .build();

        ScanPublisher scanPublisher = Mockito.mock(ScanPublisher.class);
        Publisher<ScanResponse> publisher = Flux.just(scanResponse);
        doAnswer(invocation -> {
            Subscriber<? super ScanResponse> subscriber = (Subscriber<? super ScanResponse>) invocation.getArgument(0);
            publisher.subscribe(subscriber);
            return null;
        }).when(scanPublisher).subscribe(any(Subscriber.class));
        
        when(dynamoDbClient.scanPaginator(any(ScanRequest.class)))
                .thenReturn(scanPublisher);

        // When
        Flux<Event> result = eventRepository.findUpcomingEvents(from);

        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event.getEventDate()).isAfter(from);
                    assertThat(event.getStatus()).isEqualTo(EventStatus.ACTIVE);
                })
                .verifyComplete();
    }

    private Map<String, AttributeValue> createEventItem(Event event) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(event.getEventId().value()).build());
        item.put("name", AttributeValue.builder().s(event.getName()).build());
        item.put("description", AttributeValue.builder().s(event.getDescription()).build());
        item.put("venue", AttributeValue.builder().s(event.getVenue()).build());
        item.put("eventDate", AttributeValue.builder().n(String.valueOf(event.getEventDate().getEpochSecond())).build());
        item.put("totalCapacity", AttributeValue.builder().n(String.valueOf(event.getTotalCapacity())).build());
        item.put("availableTickets", AttributeValue.builder().n(String.valueOf(event.getAvailableTickets())).build());
        item.put("reservedTickets", AttributeValue.builder().n(String.valueOf(event.getReservedTickets())).build());
        item.put("soldTickets", AttributeValue.builder().n(String.valueOf(event.getSoldTickets())).build());
        item.put("status", AttributeValue.builder().s(event.getStatus().name()).build());
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(event.getCreatedAt().getEpochSecond())).build());
        item.put("updatedAt", AttributeValue.builder().n(String.valueOf(event.getUpdatedAt().getEpochSecond())).build());
        item.put("version", AttributeValue.builder().n(String.valueOf(event.getVersion())).build());
        return item;
    }
}
