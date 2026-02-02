package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketStatus;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import com.eventticket.domain.valueobject.TicketId;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DynamoDB implementation of TicketItemRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBTicketItemRepository implements TicketItemRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTicketItemRepository.class);
    private static final String TABLE_NAME = "TicketItems";
    private static final String SEAT_RESERVATIONS_TABLE = "SeatReservations";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBTicketItemRepository(
            DynamoDbAsyncClient dynamoDbClient,
            ObjectMapper objectMapper
    ) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<TicketItem> save(TicketItem ticketItem) {
        log.debug("Saving ticket item to DynamoDB: {}", ticketItem.getTicketId().value());
        
        Map<String, AttributeValue> item = toDynamoDBItem(ticketItem);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(ticketItem))
                .doOnSuccess(t -> log.debug("Ticket item saved successfully: {}", t.getTicketId().value()))
                .doOnError(error -> log.error("Error saving ticket item to DynamoDB", error));
    }

    @Override
    public Flux<TicketItem> saveAll(List<TicketItem> ticketItems) {
        log.debug("Saving {} ticket items to DynamoDB", ticketItems.size());
        
        return Flux.fromIterable(ticketItems)
                .flatMap(this::save)
                .doOnComplete(() -> log.debug("All ticket items saved successfully"));
    }

    @Override
    public Mono<TicketItem> findById(TicketId ticketId) {
        log.debug("Finding ticket item by ID: {}", ticketId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("ticketId", AttributeValue.builder().s(ticketId.value()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .flatMap(response -> {
                    if (response.hasItem()) {
                        TicketItem ticketItem = fromDynamoDBItem(response.item());
                        return Mono.just(ticketItem);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(t -> {
                    if (t != null) {
                        log.debug("Ticket item found: {}", ticketId.value());
                    }
                })
                .doOnError(error -> log.error("Error finding ticket item in DynamoDB", error));
    }

    @Override
    public Flux<TicketItem> findByOrderId(OrderId orderId) {
        log.debug("Finding ticket items by orderId: {}", orderId.value());
        
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#orderId", "orderId");
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":orderId", AttributeValue.builder().s(orderId.value()).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("OrderIndex")
                .keyConditionExpression("#orderId = :orderId")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Mono.fromFuture(dynamoDbClient.query(request))
                .flatMapMany(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket items by orderId in DynamoDB", error));
    }

    @Override
    public Mono<Void> deleteById(TicketId ticketId) {
        log.debug("Deleting ticket item: {}", ticketId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("ticketId", AttributeValue.builder().s(ticketId.value()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.deleteItem(request))
                .then()
                .doOnSuccess(v -> log.debug("Ticket item deleted: {}", ticketId.value()))
                .doOnError(error -> log.error("Error deleting ticket item from DynamoDB", error));
    }

    @Override
    public Mono<Void> deleteByOrderId(OrderId orderId) {
        log.debug("Deleting all ticket items for order: {}", orderId.value());
        
        return findByOrderId(orderId)
                .flatMap(ticketItem -> deleteById(ticketItem.getTicketId()))
                .then()
                .doOnSuccess(v -> log.debug("All ticket items deleted for order: {}", orderId.value()));
    }

    @Override
    public Flux<TicketItem> findByReservationId(ReservationId reservationId) {
        log.debug("Finding ticket items by reservationId: {}", reservationId.value());
        
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#reservationId", "reservationId");
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":reservationId", AttributeValue.builder().s(reservationId.value()).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("ReservationIndex")
                .keyConditionExpression("#reservationId = :reservationId")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Mono.fromFuture(dynamoDbClient.query(request))
                .flatMapMany(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket items by reservationId in DynamoDB", error));
    }

    @Override
    public Flux<TicketItem> findByEventIdAndTicketTypeWithSeatNumber(
            com.eventticket.domain.valueobject.EventId eventId,
            String ticketType
    ) {
        log.debug("Finding ticket items with seat numbers by eventId={}, ticketType={}", 
                eventId.value(), ticketType);
        
        // Note: Since TicketItem doesn't have eventId directly, we need to use Scan
        // with filter. For better performance, we should add eventId to TicketItem
        // or create a GSI with eventId + ticketType + seatNumber
        
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#ticketType", "ticketType");
        expressionAttributeNames.put("#seatNumber", "seatNumber");
        expressionAttributeNames.put("#status", "status");
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":ticketType", AttributeValue.builder().s(ticketType).build());
        expressionAttributeValues.put(":sold", AttributeValue.builder().s("SOLD").build());
        expressionAttributeValues.put(":complimentary", AttributeValue.builder().s("COMPLIMENTARY").build());
        
        // Filter: ticketType = :ticketType AND seatNumber IS NOT NULL 
        // AND (status = SOLD OR status = COMPLIMENTARY)
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression(
                        "#ticketType = :ticketType " +
                        "AND attribute_exists(#seatNumber) " +
                        "AND (#status = :sold OR #status = :complimentary)"
                )
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();
        
        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .filter(ticket -> {
                    // Additional filter: only tickets from the specified event
                    // Since we don't have eventId in TicketItem, we'll need to get it from the order
                    // For now, we'll return all matching tickets and filter by eventId in the use case
                    return ticket.getSeatNumber() != null;
                })
                .doOnError(error -> log.error(
                        "Error finding ticket items with seat numbers in DynamoDB", error));
    }

    @Override
    public Mono<Void> assignSeatsAtomically(
            List<TicketItem> tickets,
            com.eventticket.domain.valueobject.EventId eventId,
            String ticketType,
            List<String> seatNumbers
    ) {
        if (tickets.size() != seatNumbers.size()) {
            return Mono.error(new IllegalArgumentException(
                    "Tickets and seat numbers lists must have the same size"));
        }

        // Validate no duplicate tickets (same ticketId) to avoid multiple operations on same item
        long uniqueTicketIds = tickets.stream()
                .map(TicketItem::getTicketId)
                .distinct()
                .count();
        if (uniqueTicketIds != tickets.size()) {
            return Mono.error(new IllegalArgumentException(
                    "Cannot assign seats: duplicate tickets found in the list. " +
                    "Each ticket must be unique."));
        }

        log.info("Assigning {} seats atomically for event {} and ticket type {}",
                seatNumbers.size(), eventId.value(), ticketType);

        List<TransactWriteItem> transactItems = new ArrayList<>();

        // For each ticket-seat pair, create:
        // 1. A ConditionCheck to verify the seat is not already reserved
        // 2. A PutItem to create the seat reservation
        // 3. A PutItem to save the ticket with the seat number
        for (int i = 0; i < tickets.size(); i++) {
            TicketItem ticket = tickets.get(i);
            String seatNumber = seatNumbers.get(i);

            // Create composite key for seat reservation: eventId#ticketType#seatNumber
            String seatKey = "%s#%s#%s".formatted(eventId.value(), ticketType, seatNumber);

            // 1. ConditionCheck: Verify seat is not already reserved
            Map<String, AttributeValue> seatKeyMap = new HashMap<>();
            seatKeyMap.put("seatKey", AttributeValue.builder().s(seatKey).build());

            ConditionCheck conditionCheck = ConditionCheck.builder()
                    .tableName(SEAT_RESERVATIONS_TABLE)
                    .key(seatKeyMap)
                    .conditionExpression("attribute_not_exists(seatKey)")
                    .build();

            transactItems.add(TransactWriteItem.builder()
                    .conditionCheck(conditionCheck)
                    .build());

            // 2. PutItem: Create seat reservation
            Map<String, AttributeValue> seatReservation = new HashMap<>();
            seatReservation.put("seatKey", AttributeValue.builder().s(seatKey).build());
            seatReservation.put("eventId", AttributeValue.builder().s(eventId.value()).build());
            seatReservation.put("ticketType", AttributeValue.builder().s(ticketType).build());
            seatReservation.put("seatNumber", AttributeValue.builder().s(seatNumber).build());
            seatReservation.put("ticketId", AttributeValue.builder().s(ticket.getTicketId().value()).build());
            seatReservation.put("orderId", AttributeValue.builder().s(ticket.getOrderId() != null 
                    ? ticket.getOrderId().value() : "").build());
            seatReservation.put("reservedAt", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond())).build());

            Put putSeatReservation = Put.builder()
                    .tableName(SEAT_RESERVATIONS_TABLE)
                    .item(seatReservation)
                    .build();

            transactItems.add(TransactWriteItem.builder()
                    .put(putSeatReservation)
                    .build());

            // 3. UpdateItem: Update ticket with seat number
            // Use UpdateItem instead of Put to avoid conflicts
            // Add condition to ensure ticket exists and doesn't already have a seat assigned
            Map<String, AttributeValue> ticketKey = new HashMap<>();
            ticketKey.put("ticketId", AttributeValue.builder().s(ticket.getTicketId().value()).build());

            // Build update expression to set seatNumber and status
            String updateExpression = "SET seatNumber = :seatNumber, #status = :status, statusChangedAt = :statusChangedAt, statusChangedBy = :statusChangedBy";
            
            // Condition: ticket must exist and not already have a seat assigned
            String conditionExpression = "attribute_exists(ticketId) AND attribute_not_exists(seatNumber)";
            
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#status", "status");
            
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":seatNumber", AttributeValue.builder().s(seatNumber).build());
            expressionAttributeValues.put(":status", AttributeValue.builder().s("SOLD").build());
            expressionAttributeValues.put(":statusChangedAt", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond())).build());
            expressionAttributeValues.put(":statusChangedBy", AttributeValue.builder().s(ticket.getStatusChangedBy()).build());

            Update updateTicket = Update.builder()
                    .tableName(TABLE_NAME)
                    .key(ticketKey)
                    .updateExpression(updateExpression)
                    .conditionExpression(conditionExpression)
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            transactItems.add(TransactWriteItem.builder()
                    .update(updateTicket)
                    .build());
        }

        // Execute transaction
        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(transactItems)
                .build();

        return Mono.fromFuture(dynamoDbClient.transactWriteItems(request))
                .then()
                .doOnSuccess(v -> log.info(
                        "Successfully assigned {} seats atomically for event {} and ticket type {}",
                        seatNumbers.size(), eventId.value(), ticketType))
                .onErrorResume(TransactionCanceledException.class, e -> {
                    log.error("Transaction cancelled while assigning seats. Reason: {}", 
                            e.cancellationReasons() != null 
                                    ? e.cancellationReasons().toString() 
                                    : "Unknown");
                    return Mono.error(new IllegalStateException(
                            "Failed to assign seats atomically. One or more seats may already be reserved.", e));
                })
                .doOnError(error -> log.error(
                        "Error assigning seats atomically for event {} and ticket type {}",
                        eventId.value(), ticketType, error));
    }

    /**
     * Converts TicketItem domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(TicketItem ticketItem) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("ticketId", AttributeValue.builder().s(ticketItem.getTicketId().value()).build());
            item.put("ticketType", AttributeValue.builder().s(ticketItem.getTicketType()).build());
            
            // OrderId is needed for GSI
            if (ticketItem.getOrderId() != null) {
                item.put("orderId", AttributeValue.builder().s(ticketItem.getOrderId().value()).build());
            }
            
            // ReservationId is needed for GSI
            if (ticketItem.getReservationId() != null) {
                item.put("reservationId", AttributeValue.builder().s(ticketItem.getReservationId().value()).build());
            }
            
            if (ticketItem.getSeatNumber() != null) {
                item.put("seatNumber", AttributeValue.builder().s(ticketItem.getSeatNumber()).build());
            }
            
            item.put("priceAmount", AttributeValue.builder().n(ticketItem.getPrice().getAmount().toString()).build());
            item.put("priceCurrency", AttributeValue.builder().s(ticketItem.getPrice().getCurrencyCode()).build());
            item.put("status", AttributeValue.builder().s(ticketItem.getStatus().name()).build());
            item.put("statusChangedAt", AttributeValue.builder().n(String.valueOf(ticketItem.getStatusChangedAt().getEpochSecond())).build());
            item.put("statusChangedBy", AttributeValue.builder().s(ticketItem.getStatusChangedBy()).build());
            
            return item;
        } catch (Exception e) {
            log.error("Error converting TicketItem to DynamoDB item", e);
            throw new RuntimeException("Failed to convert TicketItem to DynamoDB", e);
        }
    }

    /**
     * Converts DynamoDB item to TicketItem domain object.
     * Uses reflection to access private constructor.
     */
    private TicketItem fromDynamoDBItem(Map<String, AttributeValue> item) {
        try {
            TicketId ticketId = TicketId.of(item.get("ticketId").s());
            OrderId orderId = item.containsKey("orderId") && item.get("orderId") != null
                    ? OrderId.of(item.get("orderId").s())
                    : null;
            ReservationId reservationId = item.containsKey("reservationId") && item.get("reservationId") != null
                    ? ReservationId.of(item.get("reservationId").s())
                    : null;
            String ticketType = item.get("ticketType").s();
            String seatNumber = item.containsKey("seatNumber") && item.get("seatNumber") != null 
                    ? item.get("seatNumber").s() 
                    : null;
            
            BigDecimal priceAmount = new BigDecimal(item.get("priceAmount").n());
            String priceCurrency = item.get("priceCurrency").s();
            Money price = Money.of(priceAmount, priceCurrency);
            
            TicketStatus status = TicketStatus.valueOf(item.get("status").s());
            Instant statusChangedAt = Instant.ofEpochSecond(Long.parseLong(item.get("statusChangedAt").n()));
            String statusChangedBy = item.get("statusChangedBy").s();

            // Use reflection to access private constructor
            Constructor<TicketItem> constructor = TicketItem.class.getDeclaredConstructor(
                    TicketId.class, OrderId.class, ReservationId.class, String.class, String.class, Money.class,
                    TicketStatus.class, Instant.class, String.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    ticketId, orderId, reservationId, ticketType, seatNumber, price,
                    status, statusChangedAt, statusChangedBy
            );
        } catch (Exception e) {
            log.error("Error converting DynamoDB item to TicketItem", e);
            throw new RuntimeException("Failed to reconstruct TicketItem from DynamoDB", e);
        }
    }
}
