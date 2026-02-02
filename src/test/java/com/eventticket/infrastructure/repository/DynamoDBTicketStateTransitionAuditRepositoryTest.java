package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketStateTransitionAudit;
import com.eventticket.domain.model.TicketStatus;
import com.eventticket.domain.valueobject.TicketId;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDBTicketStateTransitionAuditRepository Tests")
class DynamoDBTicketStateTransitionAuditRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @InjectMocks
    private DynamoDBTicketStateTransitionAuditRepository auditRepository;

    private TicketStateTransitionAudit testAudit;
    private TicketId ticketId;

    @BeforeEach
    void setUp() {
        ticketId = TicketId.generate();
        testAudit = new TicketStateTransitionAudit(
                ticketId,
                TicketStatus.AVAILABLE,
                TicketStatus.RESERVED,
                Instant.now(),
                "system",
                "Reservation created",
                true,
                null
        );
    }


    @Test
    @DisplayName("Should save audit successfully")
    void shouldSaveAuditSuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<TicketStateTransitionAudit> result = auditRepository.save(testAudit);

        // Then
        StepVerifier.create(result)
                .assertNext(audit -> {
                    assertThat(audit.ticketId()).isEqualTo(ticketId);
                    assertThat(audit.fromStatus()).isEqualTo(TicketStatus.AVAILABLE);
                    assertThat(audit.toStatus()).isEqualTo(TicketStatus.RESERVED);
                    assertThat(audit.successful()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find audits by ticket ID")
    void shouldFindAuditsByTicketId() {
        // Given
        Map<String, AttributeValue> item = createAuditItem(testAudit);
        QueryResponse queryResponse = QueryResponse.builder()
                .items(item)
                .build();
        CompletableFuture<QueryResponse> future = CompletableFuture.completedFuture(queryResponse);

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(future);

        // When
        Flux<TicketStateTransitionAudit> result = auditRepository.findByTicketId(ticketId);

        // Then
        StepVerifier.create(result)
                .assertNext(audit -> {
                    assertThat(audit.ticketId()).isEqualTo(ticketId);
                    assertThat(audit.fromStatus()).isEqualTo(TicketStatus.AVAILABLE);
                    assertThat(audit.toStatus()).isEqualTo(TicketStatus.RESERVED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find audits by time range")
    void shouldFindAuditsByTimeRange() {
        // Given
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        
        Map<String, AttributeValue> item = createAuditItem(testAudit);
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
        Flux<TicketStateTransitionAudit> result = auditRepository.findByTimeRange(from, to);

        // Then
        StepVerifier.create(result)
                .assertNext(audit -> {
                    assertThat(audit.transitionTime()).isAfter(from);
                    assertThat(audit.transitionTime()).isBefore(to);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find failed transitions")
    void shouldFindFailedTransitions() {
        // Given
        TicketStateTransitionAudit failedAudit = new TicketStateTransitionAudit(
                ticketId,
                TicketStatus.AVAILABLE,
                TicketStatus.RESERVED,
                Instant.now(),
                "system",
                "Invalid transition",
                false,
                "Transition not allowed"
        );
        
        Map<String, AttributeValue> item = createAuditItem(failedAudit);
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
        Flux<TicketStateTransitionAudit> result = auditRepository.findFailedTransitions();

        // Then
        StepVerifier.create(result)
                .assertNext(audit -> {
                    assertThat(audit.successful()).isFalse();
                    assertThat(audit.errorMessage()).isNotNull();
                })
                .verifyComplete();
    }

    private Map<String, AttributeValue> createAuditItem(TicketStateTransitionAudit audit) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("auditId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
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
}
