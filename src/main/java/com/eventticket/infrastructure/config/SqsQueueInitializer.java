package com.eventticket.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Initializes SQS queues on application startup.
 * Creates queues if they don't exist.
 */
@Component
public class SqsQueueInitializer {

    private static final Logger log = LoggerFactory.getLogger(SqsQueueInitializer.class);

    private final SqsAsyncClient sqsClient;
    private final String queueName;

    public SqsQueueInitializer(
            SqsAsyncClient sqsClient,
            @Value("${application.sqs.order-queue-url:ticket-order-queue}") String queueName
    ) {
        this.sqsClient = sqsClient;
        this.queueName = queueName;
    }

    @PostConstruct
    public void initializeQueues() {
        log.info("Initializing SQS queues...");
        createQueueIfNotExists(queueName);
        log.info("SQS queue initialization completed");
    }

    private void createQueueIfNotExists(String queueName) {
        try {
            // Try to get queue URL to check if it exists
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();

            CompletableFuture<GetQueueUrlResponse> future = sqsClient.getQueueUrl(getQueueRequest);
            GetQueueUrlResponse response = future.join();
            
            if (response.queueUrl() != null && !response.queueUrl().isEmpty()) {
                log.info("Queue '{}' already exists: {}", queueName, response.queueUrl());
                return;
            }
        } catch (QueueDoesNotExistException e) {
            log.info("Queue '{}' does not exist, creating it...", queueName);
        } catch (Exception e) {
            log.warn("Error checking if queue exists, attempting to create: {}", e.getMessage());
        }

        // Create the queue
        try {
            Map<QueueAttributeName, String> attributes = new HashMap<>();
            attributes.put(QueueAttributeName.DELAY_SECONDS, "0");
            attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, "345600");
            attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");
            attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");

            CreateQueueRequest createRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .attributes(attributes)
                    .build();

            CompletableFuture<CreateQueueResponse> createFuture = sqsClient.createQueue(createRequest);
            CreateQueueResponse createResponse = createFuture.join();
            
            log.info("Queue '{}' created successfully: {}", queueName, createResponse.queueUrl());
        } catch (QueueNameExistsException e) {
            log.info("Queue '{}' was created by another process", queueName);
        } catch (Exception e) {
            log.error("Failed to create queue '{}': {}", queueName, e.getMessage(), e);
        }
    }
}
