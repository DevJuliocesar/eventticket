package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB implementation of TicketInventoryRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBTicketInventoryRepository implements TicketInventoryRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTicketInventoryRepository.class);
    private static final String TABLE_NAME = "TicketInventory";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBTicketInventoryRepository(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<TicketInventory> save(TicketInventory inv) {
        log.debug("Saving ticket inventory to DynamoDB: eventId={}, ticketType={}", 
                inv.getEventId().value(), inv.getTicketType());
        
        Map<String, AttributeValue> item = toDynamoDBItem(inv);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(inv))
                .doOnSuccess(i -> log.debug("Ticket inventory saved successfully: eventId={}, ticketType={}", 
                        i.getEventId().value(), i.getTicketType()))
                .doOnError(error -> log.error("Error saving ticket inventory to DynamoDB", error));
    }

    @Override
    public Mono<TicketInventory> findByEventIdAndTicketType(EventId eventId, String ticketType) {
        log.debug("Finding ticket inventory by eventId={}, ticketType={}", eventId.value(), ticketType);
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("eventId", AttributeValue.builder().s(eventId.value()).build());
        key.put("ticketType", AttributeValue.builder().s(ticketType).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .map(response -> {
                    if (response.hasItem()) {
                        return fromDynamoDBItem(response.item());
                    }
                    return null;
                })
                .cast(TicketInventory.class)
                .switchIfEmpty(Mono.empty())
                .doOnSuccess(i -> {
                    if (i != null) {
                        log.debug("Ticket inventory found: eventId={}, ticketType={}", 
                                eventId.value(), ticketType);
                    }
                })
                .doOnError(error -> log.error("Error finding ticket inventory in DynamoDB", error));
    }

    @Override
    public Flux<TicketInventory> findByEventId(EventId eventId) {
        log.debug("Finding all ticket inventory for eventId: {}", eventId.value());
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":eventId", AttributeValue.builder().s(eventId.value()).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("eventId = :eventId")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Mono.fromFuture(dynamoDbClient.query(request))
                .flatMapMany(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket inventory by eventId in DynamoDB", error));
    }

    @Override
    public Mono<TicketInventory> updateWithOptimisticLock(TicketInventory inv) {
        log.debug("Updating ticket inventory with optimistic lock: eventId={}, ticketType={}", 
                inv.getEventId().value(), inv.getTicketType());
        
        // For optimistic locking, we use PutItem with condition check
        // First, get the current version
        return findByEventIdAndTicketType(inv.getEventId(), inv.getTicketType())
                .flatMap(existing -> {
                    if (existing.getVersion() != inv.getVersion() - 1) {
                        log.warn("Optimistic lock failure for ticket inventory: eventId={}, ticketType={}. Expected version: {}, got: {}", 
                                inv.getEventId().value(), inv.getTicketType(), 
                                inv.getVersion() - 1, existing.getVersion());
                        return Mono.error(new IllegalStateException("Optimistic lock failure"));
                    }
                    
                    // Use PutItem with condition expression
                    Map<String, AttributeValue> item = toDynamoDBItem(inv);
                    Map<String, AttributeValue> key = new HashMap<>();
                    key.put("eventId", AttributeValue.builder().s(inv.getEventId().value()).build());
                    key.put("ticketType", AttributeValue.builder().s(inv.getTicketType()).build());
                    
                    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                    expressionAttributeValues.put(":expectedVersion", 
                            AttributeValue.builder().n(String.valueOf(inv.getVersion() - 1)).build());
                    
                    PutItemRequest request = PutItemRequest.builder()
                            .tableName(TABLE_NAME)
                            .item(item)
                            .conditionExpression("version = :expectedVersion")
                            .expressionAttributeValues(expressionAttributeValues)
                            .build();
                    
                    return Mono.fromFuture(dynamoDbClient.putItem(request))
                            .then(Mono.just(inv))
                            .onErrorResume(ConditionalCheckFailedException.class, e -> {
                                log.warn("Optimistic lock failure for ticket inventory: eventId={}, ticketType={}", 
                                        inv.getEventId().value(), inv.getTicketType());
                                return Mono.error(new IllegalStateException("Optimistic lock failure"));
                            });
                })
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Ticket inventory not found for update: eventId=" + inv.getEventId().value() + 
                        ", ticketType=" + inv.getTicketType())))
                .doOnError(error -> log.error("Error updating ticket inventory in DynamoDB", error));
    }

    /**
     * Converts TicketInventory domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(TicketInventory inv) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(inv.getEventId().value()).build());
        item.put("ticketType", AttributeValue.builder().s(inv.getTicketType()).build());
        item.put("eventName", AttributeValue.builder().s(inv.getEventName()).build());
        item.put("totalQuantity", AttributeValue.builder().n(String.valueOf(inv.getTotalQuantity())).build());
        item.put("availableQuantity", AttributeValue.builder().n(String.valueOf(inv.getAvailableQuantity())).build());
        item.put("reservedQuantity", AttributeValue.builder().n(String.valueOf(inv.getReservedQuantity())).build());
        item.put("priceAmount", AttributeValue.builder().n(inv.getPrice().getAmount().toString()).build());
        item.put("priceCurrency", AttributeValue.builder().s(inv.getPrice().getCurrencyCode()).build());
        item.put("version", AttributeValue.builder().n(String.valueOf(inv.getVersion())).build());
        return item;
    }

    /**
     * Converts DynamoDB item to TicketInventory domain object.
     * Uses reflection to access private constructor.
     */
    private TicketInventory fromDynamoDBItem(Map<String, AttributeValue> item) {
        try {
            EventId eventId = EventId.of(item.get("eventId").s());
            String ticketType = item.get("ticketType").s();
            String eventName = item.get("eventName").s();
            int totalQuantity = Integer.parseInt(item.get("totalQuantity").n());
            int availableQuantity = Integer.parseInt(item.get("availableQuantity").n());
            int reservedQuantity = Integer.parseInt(item.get("reservedQuantity").n());
            BigDecimal priceAmount = new BigDecimal(item.get("priceAmount").n());
            String priceCurrencyCode = item.get("priceCurrency").s();
            Money price = Money.of(priceAmount, priceCurrencyCode);
            int version = Integer.parseInt(item.get("version").n());

            // Use reflection to access private constructor
            Constructor<TicketInventory> constructor = TicketInventory.class.getDeclaredConstructor(
                    EventId.class, String.class, String.class, int.class, int.class, 
                    int.class, Money.class, int.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    eventId, ticketType, eventName, totalQuantity, 
                    availableQuantity, reservedQuantity, price, version
            );
        } catch (Exception e) {
            log.error("Error converting DynamoDB item to TicketInventory", e);
            throw new RuntimeException("Failed to reconstruct TicketInventory from DynamoDB", e);
        }
    }
}
