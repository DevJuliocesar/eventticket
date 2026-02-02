package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
@DisplayName("DynamoDBTicketOrderRepository Tests")
class DynamoDBTicketOrderRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    private ObjectMapper objectMapper;

    @InjectMocks
    private DynamoDBTicketOrderRepository orderRepository;

    private TicketOrder testOrder;
    private OrderId orderId;
    private CustomerId customerId;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        customerId = CustomerId.of("customer-123");
        eventId = EventId.generate();
        
        // Initialize ObjectMapper for JSON serialization/deserialization
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Create repository with ObjectMapper
        orderRepository = new DynamoDBTicketOrderRepository(dynamoDbClient, objectMapper);
        
        List<TicketItem> tickets = List.of(
                TicketItem.create("VIP", Money.of(100.0, "USD"))
        );
        
        testOrder = TicketOrder.create(
                customerId, eventId, "Test Event", tickets
        );
    }


    @Test
    @DisplayName("Should save order successfully")
    void shouldSaveOrderSuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketOrder> result = orderRepository.save(testOrder);

        // Then
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isEqualTo(testOrder.getOrderId());
                    assertThat(order.getCustomerId()).isEqualTo(customerId);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find order by ID when exists")
    void shouldFindOrderByIdWhenExists() {
        // Given
        Map<String, AttributeValue> item = createOrderItem(testOrder);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(item)
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketOrder> result = orderRepository.findById(testOrder.getOrderId());

        // Then
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isEqualTo(testOrder.getOrderId());
                    assertThat(order.getCustomerId()).isEqualTo(customerId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when order not found")
    void shouldReturnEmptyWhenOrderNotFound() {
        // Given
        GetItemResponse getResponse = GetItemResponse.builder().build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketOrder> result = orderRepository.findById(orderId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find orders by customer ID")
    void shouldFindOrdersByCustomerId() {
        // Given
        Map<String, AttributeValue> item = createOrderItem(testOrder);
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
        Flux<TicketOrder> result = orderRepository.findByCustomerId(customerId);

        // Then
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getCustomerId()).isEqualTo(customerId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find orders by status")
    void shouldFindOrdersByStatus() {
        // Given
        Map<String, AttributeValue> item = createOrderItem(testOrder);
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
        Flux<TicketOrder> result = orderRepository.findByStatus(OrderStatus.AVAILABLE);

        // Then
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find orders by event ID")
    void shouldFindOrdersByEventId() {
        // Given
        Map<String, AttributeValue> item = createOrderItem(testOrder);
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
        Flux<TicketOrder> result = orderRepository.findByEventId(eventId);

        // Then
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getEventId()).isEqualTo(eventId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete order by ID")
    void shouldDeleteOrderById() {
        // Given
        DeleteItemResponse deleteResponse = DeleteItemResponse.builder().build();
        CompletableFuture<DeleteItemResponse> future = CompletableFuture.completedFuture(deleteResponse);

        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Void> result = orderRepository.deleteById(orderId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    private Map<String, AttributeValue> createOrderItem(TicketOrder order) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("orderId", AttributeValue.builder().s(order.getOrderId().value()).build());
            item.put("customerId", AttributeValue.builder().s(order.getCustomerId().value()).build());
            item.put("orderNumber", AttributeValue.builder().s(order.getOrderNumber()).build());
            item.put("eventId", AttributeValue.builder().s(order.getEventId().value()).build());
            item.put("eventName", AttributeValue.builder().s(order.getEventName()).build());
            item.put("status", AttributeValue.builder().s(order.getStatus().name()).build());
            
            // Serialize tickets as JSON for backward compatibility
            String ticketsJson = objectMapper.writeValueAsString(order.getTickets());
            item.put("tickets", AttributeValue.builder().s(ticketsJson).build());
            
            item.put("totalAmount", AttributeValue.builder().n(order.getTotalAmount().getAmount().toString()).build());
            item.put("totalCurrency", AttributeValue.builder().s(order.getTotalAmount().getCurrencyCode()).build());
            item.put("createdAt", AttributeValue.builder().n(String.valueOf(order.getCreatedAt().getEpochSecond())).build());
            item.put("updatedAt", AttributeValue.builder().n(String.valueOf(order.getUpdatedAt().getEpochSecond())).build());
            item.put("version", AttributeValue.builder().n(String.valueOf(order.getVersion())).build());
            return item;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create order item for test", e);
        }
    }
}
