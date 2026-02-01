package com.eventticket.infrastructure.messaging;

import com.eventticket.application.usecase.ProcessTicketOrderUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Consumer for processing order messages from SQS queue.
 * Functional Requirement #3: Asynchronous order processing.
 * Polls the queue periodically and processes messages.
 */
@Component
public class SqsOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderConsumer.class);

    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessTicketOrderUseCase processOrderUseCase;
    private final String queueUrl;

    public SqsOrderConsumer(
            SqsAsyncClient sqsClient,
            ObjectMapper objectMapper,
            ProcessTicketOrderUseCase processOrderUseCase,
            @Value("${application.sqs.order-queue-url:ticket-order-queue}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.processOrderUseCase = processOrderUseCase;
        this.queueUrl = queueUrl;
    }

    /**
     * Polls SQS queue for messages and processes them.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelayString = "${application.sqs.poll-interval-ms:5000}")
    public void pollAndProcessMessages() {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(getQueueUrl())
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(5)
                    .build();

            CompletableFuture<ReceiveMessageResponse> future = sqsClient.receiveMessage(request);
            ReceiveMessageResponse response = future.join();

            List<Message> messages = response.messages();
            if (messages.isEmpty()) {
                log.debug("No messages in queue");
                return;
            }

            log.info("Processing {} messages from queue", messages.size());
            for (Message message : messages) {
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("Error polling SQS queue", e);
        }
    }

    private void processMessage(Message message) {
        try {
            String messageBody = message.body();
            OrderMessage orderMessage = objectMapper.readValue(messageBody, OrderMessage.class);

            log.info("Processing order message: orderId={}", orderMessage.orderId());

            processOrderUseCase.execute(orderMessage.orderId())
                    .subscribe(
                            result -> {
                                log.info("Order processed successfully: orderId={}", orderMessage.orderId());
                                deleteMessage(message);
                            },
                            error -> {
                                log.error("Error processing order: orderId={}", orderMessage.orderId(), error);
                                // Message will become visible again after visibility timeout
                                // Could implement DLQ logic here
                            }
                    );
        } catch (Exception e) {
            log.error("Error parsing message: receiptHandle={}", message.receiptHandle(), e);
            deleteMessage(message); // Delete malformed messages
        }
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(getQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteRequest)
                    .thenRun(() -> log.debug("Message deleted: receiptHandle={}", message.receiptHandle()))
                    .exceptionally(error -> {
                        log.error("Error deleting message", error);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error deleting message: receiptHandle={}", message.receiptHandle(), e);
        }
    }

    private String getQueueUrl() {
        // For LocalStack, if queueUrl doesn't start with http, get from SQS or construct
        if (queueUrl.startsWith("http")) {
            return queueUrl;
        }
        // Try to get actual queue URL from SQS (retry up to 3 times with delay)
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                var getQueueUrlRequest = software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.builder()
                        .queueName(queueUrl)
                        .build();
                var future = sqsClient.getQueueUrl(getQueueUrlRequest);
                String url = future.join().queueUrl();
                log.debug("Retrieved queue URL from SQS: {}", url);
                return url;
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    log.debug("Attempt {} failed to get queue URL, retrying... Error: {}", attempt, e.getMessage());
                    try {
                        Thread.sleep(2000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.warn("Could not get queue URL from SQS for queue '{}' after {} attempts. Error: {}", 
                            queueUrl, maxRetries, e.getMessage());
                    // LocalStack format: Use direct URL with localstack hostname
                    // From Docker container, use 'localstack' hostname instead of 'localhost'
                    String localstackHost = System.getenv().getOrDefault("LOCALSTACK_HOSTNAME", "localstack");
                    String constructedUrl = "http://%s:4566/000000000000/%s".formatted(localstackHost, queueUrl);
                    log.info("Using constructed queue URL: {}", constructedUrl);
                    return constructedUrl;
                }
            }
        }
        // Fallback: Use LocalStack's standard format
        // From Docker container, use 'localstack' hostname instead of 'localhost'
        String localstackHost = System.getenv().getOrDefault("LOCALSTACK_HOSTNAME", "localstack");
        return "http://%s:4566/000000000000/%s".formatted(localstackHost, queueUrl);
    }
}
