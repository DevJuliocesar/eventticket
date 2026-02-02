package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DynamoDB implementation of TicketOrderRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBTicketOrderRepository implements TicketOrderRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTicketOrderRepository.class);
    private static final String TABLE_NAME = "TicketOrders";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBTicketOrderRepository(
            DynamoDbAsyncClient dynamoDbClient
    ) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<TicketOrder> save(TicketOrder order) {
        log.debug("Saving ticket order to DynamoDB: {}", order.getOrderId().value());
        
        Map<String, AttributeValue> item = toDynamoDBItem(order);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(order))
                .doOnSuccess(o -> log.debug("Ticket order saved successfully: {}", o.getOrderId().value()))
                .doOnError(error -> log.error("Error saving ticket order to DynamoDB", error));
    }

    @Override
    public Mono<TicketOrder> findById(OrderId orderId) {
        log.debug("Finding ticket order by ID: {}", orderId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("orderId", AttributeValue.builder().s(orderId.value()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .flatMap(response -> {
                    if (response.hasItem()) {
                        TicketOrder order = fromDynamoDBItem(response.item());
                        return Mono.just(order);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(o -> log.debug("Ticket order found: {}", orderId.value()))
                .doOnError(error -> log.error("Error finding ticket order in DynamoDB", error));
    }

    @Override
    public Flux<TicketOrder> findByCustomerId(CustomerId customerId) {
        log.debug("Finding ticket orders by customerId: {}", customerId.value());
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":customerId", AttributeValue.builder().s(customerId.value()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("customerId = :customerId")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket orders by customerId in DynamoDB", error));
    }

    @Override
    public Flux<TicketOrder> findByStatus(OrderStatus status) {
        log.debug("Finding ticket orders by status: {}", status);
        
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":status", AttributeValue.builder().s(status.name()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("#status = :status")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket orders by status in DynamoDB", error));
    }

    @Override
    public Mono<Void> deleteById(OrderId orderId) {
        log.debug("Deleting ticket order: {}", orderId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("orderId", AttributeValue.builder().s(orderId.value()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.deleteItem(request))
                .then()
                .doOnSuccess(v -> log.debug("Ticket order deleted: {}", orderId.value()))
                .doOnError(error -> log.error("Error deleting ticket order from DynamoDB", error));
    }

    @Override
    public Flux<TicketOrder> findByEventId(EventId eventId) {
        log.debug("Finding ticket orders by eventId: {}", eventId.value());

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":eventId", AttributeValue.builder().s(eventId.value()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("eventId = :eventId")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket orders by eventId in DynamoDB", error));
    }

    /**
     * Converts TicketOrder domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(TicketOrder order) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("orderId", AttributeValue.builder().s(order.getOrderId().value()).build());
            item.put("customerId", AttributeValue.builder().s(order.getCustomerId().value()).build());
            item.put("orderNumber", AttributeValue.builder().s(order.getOrderNumber()).build());
            item.put("eventId", AttributeValue.builder().s(order.getEventId().value()).build());
            item.put("eventName", AttributeValue.builder().s(order.getEventName()).build());
            item.put("status", AttributeValue.builder().s(order.getStatus().name()).build());
            
            // Tickets are stored separately in TicketItems table, not in TicketOrders
            // No need to serialize tickets here
            
            item.put("totalAmount", AttributeValue.builder().n(order.getTotalAmount().getAmount().toString()).build());
            item.put("totalCurrency", AttributeValue.builder().s(order.getTotalAmount().getCurrencyCode()).build());
            item.put("createdAt", AttributeValue.builder().n(String.valueOf(order.getCreatedAt().getEpochSecond())).build());
            item.put("updatedAt", AttributeValue.builder().n(String.valueOf(order.getUpdatedAt().getEpochSecond())).build());
            item.put("version", AttributeValue.builder().n(String.valueOf(order.getVersion())).build());
            
            return item;
        } catch (Exception e) {
            log.error("Error converting TicketOrder to DynamoDB item", e);
            throw new RuntimeException("Failed to convert TicketOrder to DynamoDB", e);
        }
    }

    /**
     * Converts DynamoDB item to TicketOrder domain object.
     * Uses reflection to access private constructor.
     */
    private TicketOrder fromDynamoDBItem(Map<String, AttributeValue> item) {
        try {
            OrderId orderId = OrderId.of(item.get("orderId").s());
            CustomerId customerId = CustomerId.of(item.get("customerId").s());
            String orderNumber = item.get("orderNumber").s();
            EventId eventId = EventId.of(item.get("eventId").s());
            String eventName = item.get("eventName").s();
            OrderStatus status = OrderStatus.valueOf(item.get("status").s());
            
            // Tickets are stored separately in TicketItems table
            // Create order with empty tickets list - tickets will be loaded separately when needed
            List<TicketItem> tickets = List.of();
            
            BigDecimal totalAmount = new BigDecimal(item.get("totalAmount").n());
            String totalCurrency = item.get("totalCurrency").s();
            Money totalMoney = Money.of(totalAmount, totalCurrency);
            
            Instant createdAt = Instant.ofEpochSecond(Long.parseLong(item.get("createdAt").n()));
            Instant updatedAt = Instant.ofEpochSecond(Long.parseLong(item.get("updatedAt").n()));
            int version = Integer.parseInt(item.get("version").n());

            // Use reflection to access private constructor
            Constructor<TicketOrder> constructor = TicketOrder.class.getDeclaredConstructor(
                    OrderId.class, CustomerId.class, String.class, EventId.class, String.class,
                    OrderStatus.class, List.class, Money.class, Instant.class, Instant.class, int.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    orderId, customerId, orderNumber, eventId, eventName,
                    status, tickets, totalMoney, createdAt, updatedAt, version
            );
        } catch (Exception e) {
            log.error("Error converting DynamoDB item to TicketOrder", e);
            throw new RuntimeException("Failed to reconstruct TicketOrder from DynamoDB", e);
        }
    }
}
