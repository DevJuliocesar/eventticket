package com.eventticket.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.CompletableFuture;

/**
 * Initializes SNS Topic and SQS subscription on application startup.
 * Creates the SNS → SQS fan-out pattern:
 *   Publisher → SNS Topic → SQS Queue (subscription) → Consumer
 */
@Component
public class SnsTopicInitializer {

    private static final Logger log = LoggerFactory.getLogger(SnsTopicInitializer.class);

    private final SnsAsyncClient snsClient;
    private final SqsAsyncClient sqsClient;
    private final String topicName;
    private final String queueName;

    public SnsTopicInitializer(
            SnsAsyncClient snsClient,
            SqsAsyncClient sqsClient,
            @Value("${application.sns.order-topic-name:ticket-order-topic}") String topicName,
            @Value("${application.sqs.order-queue-url:ticket-order-queue}") String queueName
    ) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.topicName = topicName;
        this.queueName = queueName;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing SNS topic '{}' with SQS subscription '{}'...", topicName, queueName);
        try {
            // Step 1: Create SNS Topic (idempotent)
            String topicArn = createTopic();

            // Step 2: Get SQS Queue ARN
            String queueArn = getQueueArn();

            // Step 3: Subscribe SQS Queue to SNS Topic
            if (topicArn != null && queueArn != null) {
                subscribeSqsToSns(topicArn, queueArn);
            }

            log.info("SNS + SQS initialization completed successfully");
        } catch (Exception e) {
            log.warn("SNS + SQS initialization failed (non-fatal, will retry on first publish): {}",
                    e.getMessage());
        }
    }

    private String createTopic() {
        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();

            CompletableFuture<CreateTopicResponse> future = snsClient.createTopic(request);
            CreateTopicResponse response = future.join();
            String topicArn = response.topicArn();
            log.info("SNS Topic created/verified: {} (ARN: {})", topicName, topicArn);
            return topicArn;
        } catch (Exception e) {
            log.error("Failed to create SNS topic '{}': {}", topicName, e.getMessage());
            return null;
        }
    }

    private String getQueueArn() {
        try {
            // First get the queue URL
            String queueUrl;
            if (queueName.startsWith("http")) {
                queueUrl = queueName;
            } else {
                var getUrlRequest = software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build();
                queueUrl = sqsClient.getQueueUrl(getUrlRequest).join().queueUrl();
            }

            // Then get the queue ARN
            GetQueueAttributesRequest attrRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build();

            var attributes = sqsClient.getQueueAttributes(attrRequest).join().attributes();
            String queueArn = attributes.get(QueueAttributeName.QUEUE_ARN);
            log.info("SQS Queue ARN retrieved: {}", queueArn);
            return queueArn;
        } catch (Exception e) {
            log.error("Failed to get SQS queue ARN for '{}': {}", queueName, e.getMessage());
            return null;
        }
    }

    private void subscribeSqsToSns(String topicArn, String queueArn) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("sqs")
                    .endpoint(queueArn)
                    .attributes(java.util.Map.of("RawMessageDelivery", "true"))
                    .build();

            CompletableFuture<SubscribeResponse> future = snsClient.subscribe(request);
            SubscribeResponse response = future.join();
            log.info("SQS subscribed to SNS topic: subscriptionArn={}", response.subscriptionArn());
        } catch (Exception e) {
            log.error("Failed to subscribe SQS to SNS: {}", e.getMessage());
        }
    }
}
