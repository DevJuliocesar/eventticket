package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
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
@DisplayName("DynamoDBTicketReservationRepository Tests")
class DynamoDBTicketReservationRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @InjectMocks
    private DynamoDBTicketReservationRepository reservationRepository;

    private TicketReservation testReservation;
    private ReservationId reservationId;
    private OrderId orderId;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        reservationId = ReservationId.generate();
        orderId = OrderId.generate();
        eventId = EventId.generate();
        testReservation = TicketReservation.create(
                orderId, eventId, "VIP", 10, 10
        );
    }


    @Test
    @DisplayName("Should save reservation successfully")
    void shouldSaveReservationSuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketReservation> result = reservationRepository.save(testReservation);

        // Then
        StepVerifier.create(result)
                .assertNext(reservation -> {
                    assertThat(reservation.getReservationId()).isEqualTo(testReservation.getReservationId());
                    assertThat(reservation.getOrderId()).isEqualTo(orderId);
                    assertThat(reservation.getQuantity()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find reservation by ID when exists")
    void shouldFindReservationByIdWhenExists() {
        // Given
        Map<String, AttributeValue> item = createReservationItem(testReservation);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(item)
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketReservation> result = reservationRepository.findById(testReservation.getReservationId());

        // Then
        StepVerifier.create(result)
                .assertNext(reservation -> {
                    assertThat(reservation.getReservationId()).isEqualTo(testReservation.getReservationId());
                    assertThat(reservation.getOrderId()).isEqualTo(orderId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when reservation not found")
    void shouldReturnEmptyWhenReservationNotFound() {
        // Given
        GetItemResponse getResponse = GetItemResponse.builder().build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketReservation> result = reservationRepository.findById(reservationId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find reservations by order ID")
    void shouldFindReservationsByOrderId() {
        // Given
        Map<String, AttributeValue> item = createReservationItem(testReservation);
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
        Flux<TicketReservation> result = reservationRepository.findByOrderId(orderId);

        // Then
        StepVerifier.create(result)
                .assertNext(reservation -> {
                    assertThat(reservation.getOrderId()).isEqualTo(orderId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find expired reservations")
    void shouldFindExpiredReservations() {
        // Given
        Instant now = Instant.now();
        // Create an expired reservation (with timeout of 0 minutes, so it expires immediately)
        TicketReservation expiredReservation = TicketReservation.create(
                orderId, eventId, "VIP", 10, 0
        );
        Map<String, AttributeValue> item = createReservationItem(expiredReservation);
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
        Flux<TicketReservation> result = reservationRepository.findExpiredReservations(now);

        // Then
        StepVerifier.create(result)
                .assertNext(reservation -> {
                    assertThat(reservation.getExpiresAt()).isBefore(now);
                    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete reservation by ID")
    void shouldDeleteReservationById() {
        // Given
        DeleteItemResponse deleteResponse = DeleteItemResponse.builder().build();
        CompletableFuture<DeleteItemResponse> future = CompletableFuture.completedFuture(deleteResponse);

        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Void> result = reservationRepository.deleteById(reservationId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    private Map<String, AttributeValue> createReservationItem(TicketReservation reservation) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("reservationId", AttributeValue.builder().s(reservation.getReservationId().value()).build());
        item.put("orderId", AttributeValue.builder().s(reservation.getOrderId().value()).build());
        item.put("eventId", AttributeValue.builder().s(reservation.getEventId().value()).build());
        item.put("ticketType", AttributeValue.builder().s(reservation.getTicketType()).build());
        item.put("quantity", AttributeValue.builder().n(String.valueOf(reservation.getQuantity())).build());
        item.put("status", AttributeValue.builder().s(reservation.getStatus().name()).build());
        item.put("expiresAt", AttributeValue.builder().n(String.valueOf(reservation.getExpiresAt().getEpochSecond())).build());
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(reservation.getCreatedAt().getEpochSecond())).build());
        return item;
    }
}
