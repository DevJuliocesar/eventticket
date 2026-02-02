package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDBTicketInventoryRepository Tests")
class DynamoDBTicketInventoryRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @InjectMocks
    private DynamoDBTicketInventoryRepository inventoryRepository;

    private TicketInventory testInventory;
    private EventId eventId;

    @BeforeEach
    void setUp() {
        eventId = EventId.generate();
        testInventory = TicketInventory.create(
                eventId, "VIP", "Test Event", 100, Money.of(100.0, "USD")
        );
    }

    @Test
    @DisplayName("Should save inventory successfully")
    void shouldSaveInventorySuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketInventory> result = inventoryRepository.save(testInventory);

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory.getEventId()).isEqualTo(eventId);
                    assertThat(inventory.getTicketType()).isEqualTo("VIP");
                    assertThat(inventory.getTotalQuantity()).isEqualTo(100);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find inventory by event ID and ticket type when exists")
    void shouldFindInventoryByEventIdAndTicketTypeWhenExists() {
        // Given
        Map<String, AttributeValue> item = createInventoryItem(testInventory);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(item)
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketInventory> result = inventoryRepository.findByEventIdAndTicketType(eventId, "VIP");

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory.getEventId()).isEqualTo(eventId);
                    assertThat(inventory.getTicketType()).isEqualTo("VIP");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when inventory not found")
    void shouldReturnEmptyWhenInventoryNotFound() {
        // Given
        GetItemResponse getResponse = GetItemResponse.builder().build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketInventory> result = inventoryRepository.findByEventIdAndTicketType(eventId, "VIP");

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find all inventory for event")
    void shouldFindAllInventoryForEvent() {
        // Given
        Map<String, AttributeValue> item1 = createInventoryItem(testInventory);
        TicketInventory inventory2 = TicketInventory.create(
                eventId, "General", "Test Event", 200, Money.of(50.0, "USD")
        );
        Map<String, AttributeValue> item2 = createInventoryItem(inventory2);

        QueryResponse queryResponse = QueryResponse.builder()
                .items(item1, item2)
                .build();
        CompletableFuture<QueryResponse> future = CompletableFuture.completedFuture(queryResponse);

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(future);

        // When
        Flux<TicketInventory> result = inventoryRepository.findByEventId(eventId);

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory.getEventId()).isEqualTo(eventId);
                })
                .assertNext(inventory -> {
                    assertThat(inventory.getEventId()).isEqualTo(eventId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find inventory with pagination")
    void shouldFindInventoryWithPagination() {
        // Given
        Map<String, AttributeValue> item = createInventoryItem(testInventory);
        QueryResponse queryResponse = QueryResponse.builder()
                .items(item)
                .build();
        CompletableFuture<QueryResponse> future = CompletableFuture.completedFuture(queryResponse);

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(future);

        // When
        Flux<TicketInventory> result = inventoryRepository.findByEventId(eventId, 0, 10);

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count inventory by event ID")
    void shouldCountInventoryByEventId() {
        // Given
        QueryResponse queryResponse = QueryResponse.builder()
                .count(3)
                .build();
        CompletableFuture<QueryResponse> future = CompletableFuture.completedFuture(queryResponse);

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(future);

        // When
        Mono<Long> result = inventoryRepository.countByEventId(eventId);

        // Then
        StepVerifier.create(result)
                .assertNext(count -> {
                    assertThat(count).isEqualTo(3L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should update inventory with optimistic lock")
    void shouldUpdateInventoryWithOptimisticLock() {
        // Given
        TicketInventory reservedInventory = testInventory.reserve(10);
        TicketInventory updatedInventory = reservedInventory.confirmReservation(10);

        // Mock the existing inventory with the correct version (one less than updated)
        Map<String, AttributeValue> existingItem = createInventoryItem(reservedInventory);
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
        Mono<TicketInventory> result = inventoryRepository.updateWithOptimisticLock(updatedInventory);

        // Then
        StepVerifier.create(result)
                .assertNext(inventory -> {
                    assertThat(inventory.getVersion()).isEqualTo(updatedInventory.getVersion());
                })
                .verifyComplete();
    }

    private Map<String, AttributeValue> createInventoryItem(TicketInventory inventory) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(inventory.getEventId().value()).build());
        item.put("ticketType", AttributeValue.builder().s(inventory.getTicketType()).build());
        item.put("eventName", AttributeValue.builder().s(inventory.getEventName()).build());
        item.put("totalQuantity", AttributeValue.builder().n(String.valueOf(inventory.getTotalQuantity())).build());
        item.put("availableQuantity", AttributeValue.builder().n(String.valueOf(inventory.getAvailableQuantity())).build());
        item.put("reservedQuantity", AttributeValue.builder().n(String.valueOf(inventory.getReservedQuantity())).build());
        item.put("soldQuantity", AttributeValue.builder().n(String.valueOf(inventory.getSoldQuantity())).build());
        item.put("priceAmount", AttributeValue.builder().n(inventory.getPrice().getAmount().toString()).build());
        item.put("priceCurrency", AttributeValue.builder().s(inventory.getPrice().getCurrencyCode()).build());
        item.put("version", AttributeValue.builder().n(String.valueOf(inventory.getVersion())).build());
        return item;
    }
}
