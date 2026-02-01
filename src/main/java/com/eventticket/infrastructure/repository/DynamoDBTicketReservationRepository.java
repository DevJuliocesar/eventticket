package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.ReservationStatus;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
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
 * DynamoDB implementation of TicketReservationRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBTicketReservationRepository implements TicketReservationRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTicketReservationRepository.class);
    private static final String TABLE_NAME = "TicketReservations";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBTicketReservationRepository(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<TicketReservation> save(TicketReservation reservation) {
        log.debug("Saving ticket reservation to DynamoDB: {}", reservation.getReservationId().value());
        
        Map<String, AttributeValue> item = toDynamoDBItem(reservation);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(reservation))
                .doOnSuccess(r -> log.debug("Ticket reservation saved successfully: {}", r.getReservationId().value()))
                .doOnError(error -> log.error("Error saving ticket reservation to DynamoDB", error));
    }

    @Override
    public Mono<TicketReservation> findById(ReservationId reservationId) {
        log.debug("Finding ticket reservation by ID: {}", reservationId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("reservationId", AttributeValue.builder().s(reservationId.value()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .flatMap(response -> {
                    if (response.hasItem()) {
                        TicketReservation reservation = fromDynamoDBItem(response.item());
                        return Mono.just(reservation);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(r -> log.debug("Ticket reservation found: {}", reservationId.value()))
                .doOnError(error -> log.error("Error finding ticket reservation in DynamoDB", error));
    }

    @Override
    public Flux<TicketReservation> findByOrderId(OrderId orderId) {
        log.debug("Finding ticket reservations by orderId: {}", orderId.value());
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":orderId", AttributeValue.builder().s(orderId.value()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("orderId = :orderId")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket reservations by orderId in DynamoDB", error));
    }

    @Override
    public Flux<TicketReservation> findByStatus(ReservationStatus status) {
        log.debug("Finding ticket reservations by status: {}", status);
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":status", AttributeValue.builder().s(status.name()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("status = :status")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket reservations by status in DynamoDB", error));
    }

    @Override
    public Flux<TicketReservation> findExpiredReservations(Instant now) {
        log.debug("Finding expired ticket reservations before: {}", now);
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":now", AttributeValue.builder().n(String.valueOf(now.getEpochSecond())).build());
        expressionAttributeValues.put(":activeStatus", AttributeValue.builder().s(ReservationStatus.ACTIVE.name()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("expiresAt < :now AND status = :activeStatus")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding expired ticket reservations in DynamoDB", error));
    }

    @Override
    public Mono<Void> deleteById(ReservationId reservationId) {
        log.debug("Deleting ticket reservation: {}", reservationId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("reservationId", AttributeValue.builder().s(reservationId.value()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.deleteItem(request))
                .then()
                .doOnSuccess(v -> log.debug("Ticket reservation deleted: {}", reservationId.value()))
                .doOnError(error -> log.error("Error deleting ticket reservation from DynamoDB", error));
    }

    /**
     * Converts TicketReservation domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(TicketReservation reservation) {
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

    /**
     * Converts DynamoDB item to TicketReservation domain object.
     * Uses reflection to access private constructor.
     */
    private TicketReservation fromDynamoDBItem(Map<String, AttributeValue> item) {
        try {
            ReservationId reservationId = ReservationId.of(item.get("reservationId").s());
            OrderId orderId = OrderId.of(item.get("orderId").s());
            EventId eventId = EventId.of(item.get("eventId").s());
            String ticketType = item.get("ticketType").s();
            int quantity = Integer.parseInt(item.get("quantity").n());
            ReservationStatus status = ReservationStatus.valueOf(item.get("status").s());
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(item.get("expiresAt").n()));
            Instant createdAt = Instant.ofEpochSecond(Long.parseLong(item.get("createdAt").n()));

            // Use reflection to access private constructor
            Constructor<TicketReservation> constructor = TicketReservation.class.getDeclaredConstructor(
                    ReservationId.class, OrderId.class, EventId.class, String.class,
                    int.class, ReservationStatus.class, Instant.class, Instant.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    reservationId, orderId, eventId, ticketType,
                    quantity, status, expiresAt, createdAt
            );
        } catch (Exception e) {
            log.error("Error converting DynamoDB item to TicketReservation", e);
            throw new RuntimeException("Failed to reconstruct TicketReservation from DynamoDB", e);
        }
    }
}
