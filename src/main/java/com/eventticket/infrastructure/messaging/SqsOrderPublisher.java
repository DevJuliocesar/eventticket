package com.eventticket.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Publisher for sending order messages directly to SQS queue.
 * Functional Requirement #3: Asynchronous order processing.
 * Note: SnsOrderPublisher is now the @Primary implementation.
 * This class is kept as a fallback/alternative publisher.
 */
@Component
public class SqsOrderPublisher implements OrderMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderPublisher.class);

    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsOrderPublisher(
            SqsAsyncClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${application.sqs.order-queue-url:ticket-order-queue}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    /**
     * Publishes an order message to SQS queue for asynchronous processing.
     *
     * @param message Order message to publish
     * @return Mono that completes when message is sent
     */
    @Override
    public Mono<Void> publishOrder(OrderMessage message) {
        return Mono.fromCallable(() -> {
                    try {
                        String messageBody = objectMapper.writeValueAsString(message);
                        
                        SendMessageRequest request = SendMessageRequest.builder()
                                .queueUrl(getQueueUrl())
                                .messageBody(messageBody)
                                .build();
                        
                        CompletableFuture<SendMessageResponse> future = sqsClient.sendMessage(request);
                        return future.join();
                    } catch (Exception e) {
                        log.error("Error publishing order message to SQS: {}", message.orderId(), e);
                        throw new RuntimeException("Failed to publish order message", e);
                    }
                })
                .doOnSuccess(response -> log.info(
                        "Order message published successfully: orderId={}, messageId={}",
                        message.orderId(), response.messageId()))
                .doOnError(error -> log.error(
                        "Failed to publish order message: orderId={}", message.orderId(), error))
                .then();
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
