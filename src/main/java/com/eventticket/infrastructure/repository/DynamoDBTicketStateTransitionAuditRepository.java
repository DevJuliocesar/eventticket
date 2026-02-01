package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketStateTransitionAudit;
import com.eventticket.domain.model.TicketStatus;
import com.eventticket.domain.repository.TicketStateTransitionAuditRepository;
import com.eventticket.domain.valueobject.TicketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DynamoDB implementation of TicketStateTransitionAuditRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBTicketStateTransitionAuditRepository implements TicketStateTransitionAuditRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTicketStateTransitionAuditRepository.class);
    private static final String TABLE_NAME = "TicketStateTransitionAudit";
    private static final String GSI_TICKET_ID = "ticketId-index";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBTicketStateTransitionAuditRepository(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<TicketStateTransitionAudit> save(TicketStateTransitionAudit audit) {
        log.debug("Saving ticket state transition audit to DynamoDB: ticketId={}, from={}, to={}", 
                audit.ticketId().value(), audit.fromStatus(), audit.toStatus());
        
        Map<String, AttributeValue> item = toDynamoDBItem(audit);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(audit))
                .doOnSuccess(a -> log.debug("Ticket state transition audit saved successfully: ticketId={}", 
                        a.ticketId().value()))
                .doOnError(error -> log.error("Error saving ticket state transition audit to DynamoDB", error));
    }

    @Override
    public Flux<TicketStateTransitionAudit> findByTicketId(TicketId ticketId) {
        log.debug("Finding ticket state transition audits by ticketId: {}", ticketId.value());
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":ticketId", AttributeValue.builder().s(ticketId.value()).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName(GSI_TICKET_ID)
                .keyConditionExpression("ticketId = :ticketId")
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(true) // Order by transitionTime ascending
                .build();

        return Mono.fromFuture(dynamoDbClient.query(request))
                .flatMapMany(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket state transition audits by ticketId in DynamoDB", error));
    }

    @Override
    public Flux<TicketStateTransitionAudit> findByTimeRange(Instant from, Instant to) {
        log.debug("Finding ticket state transition audits by time range: from={}, to={}", from, to);
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":from", AttributeValue.builder().n(String.valueOf(from.getEpochSecond())).build());
        expressionAttributeValues.put(":to", AttributeValue.builder().n(String.valueOf(to.getEpochSecond())).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("transitionTime >= :from AND transitionTime <= :to")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding ticket state transition audits by time range in DynamoDB", error));
    }

    @Override
    public Flux<TicketStateTransitionAudit> findFailedTransitions() {
        log.debug("Finding failed ticket state transition audits");
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":successful", AttributeValue.builder().bool(false).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("successful = :successful")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding failed ticket state transition audits in DynamoDB", error));
    }

    /**
     * Converts TicketStateTransitionAudit domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(TicketStateTransitionAudit audit) {
        Map<String, AttributeValue> item = new HashMap<>();
        
        // Generate unique ID for each audit entry
        String auditId = UUID.randomUUID().toString();
        item.put("auditId", AttributeValue.builder().s(auditId).build());
        
        // GSI key for querying by ticketId
        item.put("ticketId", AttributeValue.builder().s(audit.ticketId().value()).build());
        
        item.put("fromStatus", AttributeValue.builder().s(audit.fromStatus().name()).build());
        item.put("toStatus", AttributeValue.builder().s(audit.toStatus().name()).build());
        item.put("transitionTime", AttributeValue.builder().n(String.valueOf(audit.transitionTime().getEpochSecond())).build());
        item.put("performedBy", AttributeValue.builder().s(audit.performedBy()).build());
        item.put("reason", AttributeValue.builder().s(audit.reason() != null ? audit.reason() : "").build());
        item.put("successful", AttributeValue.builder().bool(audit.successful()).build());
        
        if (audit.errorMessage() != null) {
            item.put("errorMessage", AttributeValue.builder().s(audit.errorMessage()).build());
        }
        
        return item;
    }

    /**
     * Converts DynamoDB item to TicketStateTransitionAudit domain object.
     */
    private TicketStateTransitionAudit fromDynamoDBItem(Map<String, AttributeValue> item) {
        TicketId ticketId = TicketId.of(item.get("ticketId").s());
        TicketStatus fromStatus = TicketStatus.valueOf(item.get("fromStatus").s());
        TicketStatus toStatus = TicketStatus.valueOf(item.get("toStatus").s());
        Instant transitionTime = Instant.ofEpochSecond(Long.parseLong(item.get("transitionTime").n()));
        String performedBy = item.get("performedBy").s();
        String reason = item.get("reason").s();
        boolean successful = item.get("successful").bool();
        
        String errorMessage = null;
        if (item.containsKey("errorMessage") && item.get("errorMessage").s() != null && !item.get("errorMessage").s().isEmpty()) {
            errorMessage = item.get("errorMessage").s();
        }
        
        return new TicketStateTransitionAudit(
                ticketId,
                fromStatus,
                toStatus,
                transitionTime,
                performedBy,
                reason,
                successful,
                errorMessage
        );
    }
}
