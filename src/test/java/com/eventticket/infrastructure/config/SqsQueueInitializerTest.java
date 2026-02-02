package com.eventticket.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqsQueueInitializer Tests")
class SqsQueueInitializerTest {

    @Mock
    private SqsAsyncClient sqsClient;

    private SqsQueueInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new SqsQueueInitializer(sqsClient, "test-queue");
    }

    @Test
    @DisplayName("Should skip creation when queue already exists")
    void shouldSkipCreationWhenQueueAlreadyExists() {
        // Given
        GetQueueUrlResponse getQueueResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/test-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(getQueueResponse));

        // When
        initializer.initializeQueues();

        // Then
        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, never()).createQueue(any(CreateQueueRequest.class));
    }

    @Test
    @DisplayName("Should create queue when it does not exist")
    void shouldCreateQueueWhenItDoesNotExist() {
        // Given
        QueueDoesNotExistException queueNotExistsException = QueueDoesNotExistException.builder()
                .message("Queue does not exist")
                .build();

        CreateQueueResponse createResponse = CreateQueueResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/test-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(queueNotExistsException));
        when(sqsClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(createResponse));

        // When
        initializer.initializeQueues();

        // Then
        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(1)).createQueue(any(CreateQueueRequest.class));
    }

    @Test
    @DisplayName("Should handle QueueNameExistsException gracefully")
    void shouldHandleQueueNameExistsExceptionGracefully() {
        // Given
        QueueDoesNotExistException queueNotExistsException = QueueDoesNotExistException.builder()
                .message("Queue does not exist")
                .build();

        QueueNameExistsException queueExistsException = QueueNameExistsException.builder()
                .message("Queue already exists")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(queueNotExistsException));
        when(sqsClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(queueExistsException));

        // When
        initializer.initializeQueues();

        // Then - Should not throw exception
        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(1)).createQueue(any(CreateQueueRequest.class));
    }

    @Test
    @DisplayName("Should handle generic exception when checking queue existence")
    void shouldHandleGenericExceptionWhenCheckingQueueExistence() {
        // Given
        RuntimeException genericException = new RuntimeException("Connection error");

        CreateQueueResponse createResponse = CreateQueueResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/test-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(genericException));
        when(sqsClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(createResponse));

        // When
        initializer.initializeQueues();

        // Then - Should attempt to create queue
        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(1)).createQueue(any(CreateQueueRequest.class));
    }

    @Test
    @DisplayName("Should handle exception when creating queue")
    void shouldHandleExceptionWhenCreatingQueue() {
        // Given
        QueueDoesNotExistException queueNotExistsException = QueueDoesNotExistException.builder()
                .message("Queue does not exist")
                .build();

        RuntimeException createException = new RuntimeException("Failed to create queue");

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(queueNotExistsException));
        when(sqsClient.createQueue(any(CreateQueueRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(createException));

        // When
        initializer.initializeQueues();

        // Then - Should not throw exception, just log error
        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(1)).createQueue(any(CreateQueueRequest.class));
    }

    @Test
    @DisplayName("Should use provided queue name")
    void shouldUseProvidedQueueName() {
        // Given
        String customQueueName = "custom-queue";
        SqsQueueInitializer customInitializer = new SqsQueueInitializer(sqsClient, customQueueName);

        GetQueueUrlResponse getQueueResponse = GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/custom-queue")
                .build();

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(getQueueResponse));

        // When
        customInitializer.initializeQueues();

        // Then
        ArgumentCaptor<GetQueueUrlRequest> requestCaptor = ArgumentCaptor.forClass(GetQueueUrlRequest.class);
        verify(sqsClient).getQueueUrl(requestCaptor.capture());
        assertThat(requestCaptor.getValue().queueName()).isEqualTo(customQueueName);
    }
}
