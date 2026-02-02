package com.eventticket.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqsOrderPublisher Tests")
class SqsOrderPublisherTest {

    @Mock
    private SqsAsyncClient sqsClient;

    private ObjectMapper objectMapper;
    private SqsOrderPublisher publisher;

    private OrderMessage testMessage;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        publisher = new SqsOrderPublisher(sqsClient, objectMapper, "ticket-order-queue");

        testMessage = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should publish order message successfully")
    void shouldPublishOrderMessageSuccessfully() {
        // Given
        SendMessageResponse sendResponse = SendMessageResponse.builder()
                .messageId("msg-123")
                .build();
        CompletableFuture<SendMessageResponse> sendFuture = CompletableFuture.completedFuture(sendResponse);

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();
        CompletableFuture<GetQueueUrlResponse> queueUrlFuture = CompletableFuture.completedFuture(queueUrlResponse);

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(queueUrlFuture);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(sendFuture);

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error when publishing fails")
    void shouldHandleErrorWhenPublishingFails() {
        // Given
        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();
        CompletableFuture<GetQueueUrlResponse> queueUrlFuture = CompletableFuture.completedFuture(queueUrlResponse);

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(queueUrlFuture);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS error")));

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should use constructed URL when queue URL cannot be retrieved")
    void shouldUseConstructedUrlWhenQueueUrlCannotBeRetrieved() {
        // Given
        SendMessageResponse sendResponse = SendMessageResponse.builder()
                .messageId("msg-123")
                .build();
        CompletableFuture<SendMessageResponse> sendFuture = CompletableFuture.completedFuture(sendResponse);

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Queue not found")));
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(sendFuture);

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}
