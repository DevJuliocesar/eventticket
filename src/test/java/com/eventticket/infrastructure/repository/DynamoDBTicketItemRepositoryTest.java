package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketStatus;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.TicketId;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDBTicketItemRepository Tests")
class DynamoDBTicketItemRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DynamoDBTicketItemRepository ticketItemRepository;

    private TicketItem testTicket;
    private TicketId ticketId;
    private OrderId orderId;

    @BeforeEach
    void setUp() {
        ticketId = TicketId.generate();
        orderId = OrderId.generate();
        testTicket = TicketItem.create("VIP", Money.of(100.0, "USD"));
    }

    @Test
    @DisplayName("Should save ticket item successfully")
    void shouldSaveTicketItemSuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketItem> result = ticketItemRepository.save(testTicket);

        // Then
        StepVerifier.create(result)
                .assertNext(ticket -> {
                    assertThat(ticket.getTicketId()).isEqualTo(testTicket.getTicketId());
                    assertThat(ticket.getTicketType()).isEqualTo("VIP");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should save all ticket items")
    void shouldSaveAllTicketItems() {
        // Given
        List<TicketItem> tickets = List.of(
                testTicket,
                TicketItem.create("General", Money.of(50.0, "USD"))
        );

        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Flux<TicketItem> result = ticketItemRepository.saveAll(tickets);

        // Then
        StepVerifier.create(result)
                .assertNext(ticket -> {
                    assertThat(ticket).isNotNull();
                })
                .assertNext(ticket -> {
                    assertThat(ticket).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find ticket item by ID when exists")
    void shouldFindTicketItemByIdWhenExists() {
        // Given
        Map<String, AttributeValue> item = createTicketItem(testTicket);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(item)
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketItem> result = ticketItemRepository.findById(testTicket.getTicketId());

        // Then
        StepVerifier.create(result)
                .assertNext(ticket -> {
                    assertThat(ticket.getTicketId()).isEqualTo(testTicket.getTicketId());
                    assertThat(ticket.getTicketType()).isEqualTo("VIP");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when ticket item not found")
    void shouldReturnEmptyWhenTicketItemNotFound() {
        // Given
        GetItemResponse getResponse = GetItemResponse.builder().build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketItem> result = ticketItemRepository.findById(ticketId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find ticket items by order ID")
    void shouldFindTicketItemsByOrderId() {
        // Given
        TicketItem ticketWithOrder = testTicket.withOrderId(orderId);
        Map<String, AttributeValue> item = createTicketItem(ticketWithOrder);
        
        QueryResponse queryResponse = QueryResponse.builder()
                .items(item)
                .build();
        CompletableFuture<QueryResponse> future = CompletableFuture.completedFuture(queryResponse);

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(future);

        // When
        Flux<TicketItem> result = ticketItemRepository.findByOrderId(orderId);

        // Then
        StepVerifier.create(result)
                .assertNext(ticket -> {
                    assertThat(ticket.getOrderId()).isEqualTo(orderId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find ticket items by event ID and ticket type with seat number")
    void shouldFindTicketItemsByEventIdAndTicketTypeWithSeatNumber() {
        // Given
        // Ticket must go through valid state transitions: AVAILABLE -> RESERVED -> PENDING_CONFIRMATION -> SOLD
        TicketItem ticketWithSeat = testTicket
                .reserve("user-123")
                .confirmPayment("user-123")
                .markAsSold("customer-123", "A-1");
        Map<String, AttributeValue> item = createTicketItem(ticketWithSeat);
        
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
        Flux<TicketItem> result = ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(
                com.eventticket.domain.valueobject.EventId.generate(), "VIP"
        );

        // Then
        StepVerifier.create(result)
                .assertNext(ticket -> {
                    assertThat(ticket.getSeatNumber()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should assign seats atomically")
    void shouldAssignSeatsAtomically() {
        // Given
        List<TicketItem> tickets = List.of(
                testTicket.withOrderId(orderId)
        );
        List<String> seatNumbers = List.of("A-1");

        TransactWriteItemsResponse transactResponse = TransactWriteItemsResponse.builder().build();
        CompletableFuture<TransactWriteItemsResponse> future = CompletableFuture.completedFuture(transactResponse);

        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenReturn(future);

        // When
        Mono<Void> result = ticketItemRepository.assignSeatsAtomically(
                tickets,
                com.eventticket.domain.valueobject.EventId.generate(),
                "VIP",
                seatNumbers
        );

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    private Map<String, AttributeValue> createTicketItem(TicketItem ticket) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("ticketId", AttributeValue.builder().s(ticket.getTicketId().value()).build());
        item.put("ticketType", AttributeValue.builder().s(ticket.getTicketType()).build());
        item.put("priceAmount", AttributeValue.builder().n(ticket.getPrice().getAmount().toString()).build());
        item.put("priceCurrency", AttributeValue.builder().s(ticket.getPrice().getCurrencyCode()).build());
        item.put("status", AttributeValue.builder().s(ticket.getStatus().name()).build());
        
        if (ticket.getOrderId() != null) {
            item.put("orderId", AttributeValue.builder().s(ticket.getOrderId().value()).build());
        }
        if (ticket.getSeatNumber() != null) {
            item.put("seatNumber", AttributeValue.builder().s(ticket.getSeatNumber()).build());
        }
        if (ticket.getReservationId() != null) {
            item.put("reservationId", AttributeValue.builder().s(ticket.getReservationId().value()).build());
        }
        
        item.put("statusChangedAt", AttributeValue.builder().n(String.valueOf(ticket.getStatusChangedAt().getEpochSecond())).build());
        if (ticket.getStatusChangedBy() != null) {
            item.put("statusChangedBy", AttributeValue.builder().s(ticket.getStatusChangedBy()).build());
        }
        
        return item;
    }
}
