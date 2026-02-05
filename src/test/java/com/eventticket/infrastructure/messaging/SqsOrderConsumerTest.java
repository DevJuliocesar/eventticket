package com.eventticket.infrastructure.messaging;

import com.eventticket.application.usecase.ProcessTicketOrderUseCase;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
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
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqsOrderConsumer Tests (Reactive)")
class SqsOrderConsumerTest {

    @Mock
    private SqsAsyncClient sqsClient;

    @Mock
    private ProcessTicketOrderUseCase processOrderUseCase;

    private ObjectMapper objectMapper;
    private SqsOrderConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Note: We don't call startPolling() in tests - we test pollOnce() directly
        consumer = new SqsOrderConsumer(sqsClient, objectMapper, processOrderUseCase,
                "http://localhost:4566/000000000000/ticket-order-queue", 10, 20);
    }

    @Test
    @DisplayName("Should poll and process messages reactively")
    void shouldPollAndProcessMessagesReactively() throws Exception {
        // Given
        OrderMessage orderMessage = new OrderMessage(
                "order-123", "event-456", "customer-789", "VIP", 2, Instant.now()
        );
        String messageBody = objectMapper.writeValueAsString(orderMessage);

        Message sqsMessage = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-handle-123")
                .body(messageBody)
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        TicketOrder processedOrder = TicketOrder.create(
                CustomerId.of("customer-789"), EventId.of("event-456"),
                "Test Event", List.of()
        );

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute("order-123"))
                .thenReturn(Mono.just(processedOrder));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

        verify(processOrderUseCase).execute("order-123");
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle empty queue returning zero")
    void shouldHandleEmptyQueueReturningZero() {
        // Given
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(List.of())
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(0)
                .verifyComplete();

        verify(processOrderUseCase, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should unwrap SNS envelope messages")
    void shouldUnwrapSnsEnvelopeMessages() throws Exception {
        // Given - SNS wraps messages in an envelope with "Type" and "Message" fields
        OrderMessage orderMessage = new OrderMessage(
                "order-sns-123", "event-456", "customer-789", "VIP", 1, Instant.now()
        );
        String innerMessage = objectMapper.writeValueAsString(orderMessage);

        // SNS envelope format
        String snsEnvelope = """
                {
                    "Type": "Notification",
                    "MessageId": "sns-msg-id",
                    "TopicArn": "arn:aws:sns:us-east-1:000000000000:ticket-order-topic",
                    "Message": %s,
                    "Timestamp": "2026-01-01T00:00:00.000Z"
                }
                """.formatted(objectMapper.writeValueAsString(innerMessage));

        Message sqsMessage = Message.builder()
                .messageId("msg-456")
                .receiptHandle("receipt-456")
                .body(snsEnvelope)
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute("order-sns-123"))
                .thenReturn(Mono.just(TicketOrder.create(
                        CustomerId.of("customer-789"), EventId.of("event-456"),
                        "Test Event", List.of()
                )));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

        verify(processOrderUseCase).execute("order-sns-123");
    }

    @Test
    @DisplayName("Should delete malformed messages")
    void shouldDeleteMalformedMessages() {
        // Given
        Message sqsMessage = Message.builder()
                .messageId("msg-bad")
                .receiptHandle("receipt-bad")
                .body("invalid json")
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

        verify(processOrderUseCase, never()).execute(anyString());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should delete messages with empty body")
    void shouldDeleteMessagesWithEmptyBody() {
        // Given
        Message sqsMessage = Message.builder()
                .messageId("msg-empty")
                .receiptHandle("receipt-empty")
                .body("")
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

        verify(processOrderUseCase, never()).execute(anyString());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle processing errors gracefully without deleting message")
    void shouldHandleProcessingErrorsGracefully() throws Exception {
        // Given
        OrderMessage orderMessage = new OrderMessage(
                "order-fail", "event-456", "customer-789", "VIP", 2, Instant.now()
        );
        String messageBody = objectMapper.writeValueAsString(orderMessage);

        Message sqsMessage = Message.builder()
                .messageId("msg-fail")
                .receiptHandle("receipt-fail")
                .body(messageBody)
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute("order-fail"))
                .thenReturn(Mono.error(new RuntimeException("Processing failed")));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

        verify(processOrderUseCase).execute("order-fail");
        // Message should NOT be deleted on processing error (will become visible again)
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle polling errors returning zero")
    void shouldHandlePollingErrorsReturningZero() {
        // Given
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS error")));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(0)
                .verifyComplete();

        verify(processOrderUseCase, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should process multiple messages in a single poll")
    void shouldProcessMultipleMessagesInSinglePoll() throws Exception {
        // Given
        OrderMessage msg1 = new OrderMessage("order-1", "event-1", "customer-1", "VIP", 1, Instant.now());
        OrderMessage msg2 = new OrderMessage("order-2", "event-2", "customer-2", "General", 2, Instant.now());

        Message sqsMsg1 = Message.builder()
                .messageId("msg-1").receiptHandle("receipt-1")
                .body(objectMapper.writeValueAsString(msg1)).build();
        Message sqsMsg2 = Message.builder()
                .messageId("msg-2").receiptHandle("receipt-2")
                .body(objectMapper.writeValueAsString(msg2)).build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMsg1, sqsMsg2)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute(anyString()))
                .thenReturn(Mono.just(TicketOrder.create(
                        CustomerId.of("customer-1"), EventId.generate(),
                        "Test Event", List.of()
                )));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(2)
                .verifyComplete();

        verify(processOrderUseCase, times(2)).execute(anyString());
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should use queue name resolution for non-URL queue names")
    void shouldUseQueueNameResolutionForNonUrlQueueNames() throws Exception {
        // Given - consumer with queue name instead of URL
        consumer = new SqsOrderConsumer(sqsClient, objectMapper, processOrderUseCase,
                "ticket-order-queue", 10, 20);

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(List.of())
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));

        // When
        Mono<Integer> result = consumer.pollOnce();

        // Then
        StepVerifier.create(result)
                .expectNext(0)
                .verifyComplete();
    }
}
