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
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnsOrderPublisher Tests")
class SnsOrderPublisherTest {

    @Mock
    private SnsAsyncClient snsClient;

    private ObjectMapper objectMapper;
    private SnsOrderPublisher publisher;

    private OrderMessage testMessage;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        publisher = new SnsOrderPublisher(snsClient, objectMapper, "ticket-order-topic");

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
    @DisplayName("Should publish order message to SNS topic successfully")
    void shouldPublishOrderMessageToSnsSuccessfully() {
        // Given
        ListTopicsResponse listResponse = ListTopicsResponse.builder()
                .topics(Topic.builder()
                        .topicArn("arn:aws:sns:us-east-1:000000000000:ticket-order-topic")
                        .build())
                .build();

        PublishResponse publishResponse = PublishResponse.builder()
                .messageId("sns-msg-123")
                .build();

        when(snsClient.listTopics())
                .thenReturn(CompletableFuture.completedFuture(listResponse));
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(publishResponse));

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("Should handle error when publishing to SNS fails")
    void shouldHandleErrorWhenPublishingToSnsFails() {
        // Given
        ListTopicsResponse listResponse = ListTopicsResponse.builder()
                .topics(Topic.builder()
                        .topicArn("arn:aws:sns:us-east-1:000000000000:ticket-order-topic")
                        .build())
                .build();

        when(snsClient.listTopics())
                .thenReturn(CompletableFuture.completedFuture(listResponse));
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SNS error")));

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should use constructed ARN when topic listing fails")
    void shouldUseConstructedArnWhenTopicListingFails() {
        // Given
        when(snsClient.listTopics())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("List failed")));

        PublishResponse publishResponse = PublishResponse.builder()
                .messageId("sns-msg-456")
                .build();
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(publishResponse));

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("Should use topic ARN directly when provided as ARN")
    void shouldUseTopicArnDirectlyWhenProvidedAsArn() {
        // Given
        publisher = new SnsOrderPublisher(snsClient, objectMapper,
                "arn:aws:sns:us-east-1:123456789:my-topic");

        PublishResponse publishResponse = PublishResponse.builder()
                .messageId("sns-msg-789")
                .build();
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(publishResponse));

        // When
        Mono<Void> result = publisher.publishOrder(testMessage);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Should not try to list topics when ARN is provided directly
        verify(snsClient, never()).listTopics();
    }
}
