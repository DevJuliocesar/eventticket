package com.eventticket.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Publisher for sending order messages to SNS Topic.
 * SNS fans out messages to subscribed SQS queues (SNS + SQS pattern).
 * Functional Requirement #3: Asynchronous order processing via event-driven messaging.
 * 
 * Architecture:
 *   Publisher → SNS Topic → SQS Queue (subscription) → Reactive Consumer
 * 
 * Benefits over direct SQS publishing:
 *   - Fan-out: Multiple subscribers can receive the same event
 *   - Decoupling: Publisher doesn't need to know about specific queues
 *   - Extensibility: Add notification, analytics consumers without changing publisher
 */
@Component
@Primary
public class SnsOrderPublisher implements OrderMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(SnsOrderPublisher.class);

    private final SnsAsyncClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public SnsOrderPublisher(
            SnsAsyncClient snsClient,
            ObjectMapper objectMapper,
            @Value("${application.sns.order-topic-name:ticket-order-topic}") String topicName
    ) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    /**
     * Publishes an order message to SNS Topic for fan-out processing.
     * SNS automatically delivers the message to all subscribed SQS queues.
     *
     * @param message Order message to publish
     * @return Mono that completes when message is published to SNS
     */
    @Override
    public Mono<Void> publishOrder(OrderMessage message) {
        return Mono.fromCallable(() -> {
                    try {
                        String messageBody = objectMapper.writeValueAsString(message);

                        PublishRequest request = PublishRequest.builder()
                                .topicArn(getTopicArn())
                                .message(messageBody)
                                .subject("TicketOrder")
                                .messageAttributes(java.util.Map.of(
                                        "eventType", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue("ORDER_CREATED")
                                                .build(),
                                        "eventId", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(message.eventId())
                                                .build()
                                ))
                                .build();

                        CompletableFuture<PublishResponse> future = snsClient.publish(request);
                        return future.join();
                    } catch (Exception e) {
                        log.error("Error publishing order message to SNS: orderId={}", message.orderId(), e);
                        throw new RuntimeException("Failed to publish order message to SNS", e);
                    }
                })
                .doOnSuccess(response -> log.info(
                        "Order message published to SNS successfully: orderId={}, messageId={}",
                        message.orderId(), response.messageId()))
                .doOnError(error -> log.error(
                        "Failed to publish order message to SNS: orderId={}", message.orderId(), error))
                .then();
    }

    /**
     * Resolves the SNS Topic ARN.
     * For LocalStack, constructs the ARN from the topic name.
     */
    private String getTopicArn() {
        if (topicName.startsWith("arn:")) {
            return topicName;
        }
        // Try to get the topic ARN by listing topics
        try {
            var listResult = snsClient.listTopics().join();
            return listResult.topics().stream()
                    .filter(t -> t.topicArn().endsWith(":" + topicName))
                    .findFirst()
                    .map(t -> t.topicArn())
                    .orElseGet(() -> {
                        // Construct ARN for LocalStack
                        String localstackArn = "arn:aws:sns:us-east-1:000000000000:%s".formatted(topicName);
                        log.debug("Topic not found in listing, using constructed ARN: {}", localstackArn);
                        return localstackArn;
                    });
        } catch (Exception e) {
            // Fallback: construct ARN for LocalStack
            String fallbackArn = "arn:aws:sns:us-east-1:000000000000:%s".formatted(topicName);
            log.warn("Could not list SNS topics, using constructed ARN: {}. Error: {}", fallbackArn, e.getMessage());
            return fallbackArn;
        }
    }
}
