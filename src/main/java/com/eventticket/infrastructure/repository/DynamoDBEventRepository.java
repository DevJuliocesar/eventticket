package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.valueobject.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB implementation of EventRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBEventRepository implements EventRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBEventRepository.class);
    private static final String TABLE_NAME = "EventAggregates";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBEventRepository(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<Event> save(Event event) {
        log.debug("Saving event to DynamoDB: {}", event.getEventId().value());
        
        Map<String, AttributeValue> item = toDynamoDBItem(event);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(event))
                .doOnSuccess(e -> log.debug("Event saved successfully: {}", e.getEventId().value()))
                .doOnError(error -> log.error("Error saving event to DynamoDB", error));
    }

    @Override
    public Mono<Event> findById(EventId eventId) {
        log.debug("Finding event by ID: {}", eventId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("eventId", AttributeValue.builder().s(eventId.value()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .flatMap(response -> {
                    if (response.hasItem()) {
                        Event event = fromDynamoDBItem(response.item());
                        return Mono.just(event);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(e -> log.debug("Event found: {}", eventId.value()))
                .doOnError(error -> log.error("Error finding event in DynamoDB", error));
    }

    @Override
    public Flux<Event> findByStatus(EventStatus status) {
        log.debug("Finding events by status: {}", status);
        
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
                .doOnError(error -> log.error("Error finding events by status in DynamoDB", error));
    }

    @Override
    public Flux<Event> findUpcomingEvents(Instant from) {
        log.debug("Finding upcoming events from: {}", from);
        
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#status", "status");
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":fromDate", AttributeValue.builder().n(String.valueOf(from.getEpochSecond())).build());
        expressionAttributeValues.put(":activeStatus", AttributeValue.builder().s(EventStatus.ACTIVE.name()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("eventDate > :fromDate AND #status = :activeStatus")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding upcoming events in DynamoDB", error));
    }

    @Override
    public Mono<Event> updateWithOptimisticLock(Event event) {
        log.debug("Updating event with optimistic lock: {}", event.getEventId().value());
        
        // For optimistic locking, we use PutItem with condition check
        // First, get the current version
        return findById(event.getEventId())
                .flatMap(existing -> {
                    if (existing.getVersion() != event.getVersion() - 1) {
                        log.warn("Optimistic lock failure for event: {}. Expected version: {}, got: {}", 
                                event.getEventId().value(), event.getVersion() - 1, existing.getVersion());
                        return Mono.error(new IllegalStateException("Optimistic lock failure"));
                    }
                    
                    // Use PutItem with condition expression
                    Map<String, AttributeValue> item = toDynamoDBItem(event);
                    Map<String, AttributeValue> key = new HashMap<>();
                    key.put("eventId", AttributeValue.builder().s(event.getEventId().value()).build());
                    
                    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                    expressionAttributeValues.put(":expectedVersion", AttributeValue.builder().n(String.valueOf(event.getVersion() - 1)).build());
                    
                    PutItemRequest request = PutItemRequest.builder()
                            .tableName(TABLE_NAME)
                            .item(item)
                            .conditionExpression("version = :expectedVersion")
                            .expressionAttributeValues(expressionAttributeValues)
                            .build();
                    
                    return Mono.fromFuture(dynamoDbClient.putItem(request))
                            .then(Mono.just(event))
                            .onErrorResume(ConditionalCheckFailedException.class, e -> {
                                log.warn("Optimistic lock failure for event: {}", event.getEventId().value());
                                return Mono.error(new IllegalStateException("Optimistic lock failure"));
                            });
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Event not found for update: " + event.getEventId().value())))
                .doOnError(error -> log.error("Error updating event in DynamoDB", error));
    }

    @Override
    public Mono<Void> deleteById(EventId eventId) {
        log.debug("Deleting event: {}", eventId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("eventId", AttributeValue.builder().s(eventId.value()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.deleteItem(request))
                .then()
                .doOnSuccess(v -> log.debug("Event deleted: {}", eventId.value()))
                .doOnError(error -> log.error("Error deleting event from DynamoDB", error));
    }

    /**
     * Converts Event domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(Event event) {
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

    /**
     * Converts DynamoDB item to Event domain object.
     * Uses reflection to access private constructor.
     */
    private Event fromDynamoDBItem(Map<String, AttributeValue> item) {
        try {
            EventId eventId = EventId.of(item.get("eventId").s());
            String name = item.get("name").s();
            String description = item.get("description").s();
            String venue = item.get("venue").s();
            Instant eventDate = Instant.ofEpochSecond(Long.parseLong(item.get("eventDate").n()));
            int totalCapacity = Integer.parseInt(item.get("totalCapacity").n());
            int availableTickets = Integer.parseInt(item.get("availableTickets").n());
            int reservedTickets = Integer.parseInt(item.get("reservedTickets").n());
            int soldTickets = Integer.parseInt(item.get("soldTickets").n());
            EventStatus status = EventStatus.valueOf(item.get("status").s());
            Instant createdAt = Instant.ofEpochSecond(Long.parseLong(item.get("createdAt").n()));
            Instant updatedAt = Instant.ofEpochSecond(Long.parseLong(item.get("updatedAt").n()));
            int version = Integer.parseInt(item.get("version").n());

            // Use reflection to access private constructor
            Constructor<Event> constructor = Event.class.getDeclaredConstructor(
                    EventId.class, String.class, String.class, String.class, Instant.class,
                    int.class, int.class, int.class, int.class,
                    EventStatus.class, Instant.class, Instant.class, int.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    eventId, name, description, venue, eventDate,
                    totalCapacity, availableTickets, reservedTickets, soldTickets,
                    status, createdAt, updatedAt, version
            );
        } catch (Exception e) {
            log.error("Error converting DynamoDB item to Event", e);
            throw new RuntimeException("Failed to reconstruct Event from DynamoDB", e);
        }
    }
}
