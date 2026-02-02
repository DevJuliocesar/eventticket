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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqsOrderConsumer Tests")
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

        consumer = new SqsOrderConsumer(sqsClient, objectMapper, processOrderUseCase, "ticket-order-queue");
    }

    @Test
    @DisplayName("Should poll and process messages successfully")
    void shouldPollAndProcessMessagesSuccessfully() throws Exception {
        // Given
        OrderMessage orderMessage = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
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

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        TicketOrder processedOrder = TicketOrder.create(
                CustomerId.of("customer-789"),
                EventId.of("event-456"),
                "Test Event",
                List.of()
        );

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute("order-123"))
                .thenReturn(Mono.just(processedOrder));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, times(1)).execute("order-123");
        verify(sqsClient, atLeastOnce()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle empty queue gracefully")
    void shouldHandleEmptyQueueGracefully() {
        // Given
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(List.of())
                .build();

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should delete malformed messages")
    void shouldDeleteMalformedMessages() throws Exception {
        // Given
        String invalidMessageBody = "invalid json";

        Message sqsMessage = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-handle-123")
                .body(invalidMessageBody)
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, never()).execute(anyString());
        verify(sqsClient, atLeastOnce()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should delete messages with empty body")
    void shouldDeleteMessagesWithEmptyBody() {
        // Given
        Message sqsMessage = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-handle-123")
                .body("")
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, never()).execute(anyString());
        verify(sqsClient, atLeastOnce()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle processing errors gracefully")
    void shouldHandleProcessingErrorsGracefully() throws Exception {
        // Given
        OrderMessage orderMessage = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
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

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute("order-123"))
                .thenReturn(Mono.error(new RuntimeException("Processing failed")));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, times(1)).execute("order-123");
        // Message should not be deleted on processing error (will become visible again)
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle ValueInstantiationException for invalid message format")
    void shouldHandleValueInstantiationExceptionForInvalidMessageFormat() throws Exception {
        // Given - message missing required fields
        String invalidMessageBody = "{\"orderId\":\"order-123\"}"; // Missing other required fields

        Message sqsMessage = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-handle-123")
                .body(invalidMessageBody)
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, never()).execute(anyString());
        verify(sqsClient, atLeastOnce()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("Should handle polling errors gracefully")
    void shouldHandlePollingErrorsGracefully() {
        // Given
        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS error")));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, never()).execute(anyString());
        // Should not throw exception, just log error
    }

    @Test
    @DisplayName("Should use constructed URL when queue URL cannot be retrieved")
    void shouldUseConstructedUrlWhenQueueUrlCannotBeRetrieved() throws Exception {
        // Given
        OrderMessage orderMessage = new OrderMessage(
                "order-123",
                "event-456",
                "customer-789",
                "VIP",
                2,
                Instant.now()
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

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Queue not found")));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute("order-123"))
                .thenReturn(Mono.just(TicketOrder.create(
                        CustomerId.of("customer-789"),
                        EventId.of("event-456"),
                        "Test Event",
                        List.of()
                )));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        consumer.pollAndProcessMessages();

        // Then
        ArgumentCaptor<ReceiveMessageRequest> requestCaptor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().queueUrl()).contains("localstack");
        verify(processOrderUseCase, times(1)).execute("order-123");
    }

    @Test
    @DisplayName("Should process multiple messages")
    void shouldProcessMultipleMessages() throws Exception {
        // Given
        OrderMessage message1 = new OrderMessage("order-1", "event-1", "customer-1", "VIP", 1, Instant.now());
        OrderMessage message2 = new OrderMessage("order-2", "event-2", "customer-2", "General", 2, Instant.now());

        Message sqsMessage1 = Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body(objectMapper.writeValueAsString(message1))
                .build();

        Message sqsMessage2 = Message.builder()
                .messageId("msg-2")
                .receiptHandle("receipt-2")
                .body(objectMapper.writeValueAsString(message2))
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage1, sqsMessage2)
                .build();

        GetQueueUrlResponse queueUrlResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/ticket-order-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(queueUrlResponse));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(processOrderUseCase.execute(anyString()))
                .thenReturn(Mono.just(TicketOrder.create(
                        CustomerId.of("customer-1"),
                        EventId.generate(),
                        "Test Event",
                        List.of()
                )));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        // When
        consumer.pollAndProcessMessages();

        // Then
        verify(processOrderUseCase, times(2)).execute(anyString());
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }
}
