package com.eventticket.infrastructure.messaging;

import com.eventticket.application.usecase.ProcessTicketOrderUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.List;

/**
 * Reactive consumer for processing order messages from SQS queue.
 * Functional Requirement #3: Asynchronous order processing.
 * 
 * Architecture (SNS + SQS pattern):
 *   SnsOrderPublisher → SNS Topic → SQS Queue (subscription) → This Consumer → ProcessTicketOrderUseCase
 * 
 * Uses reactive continuous long-polling instead of @Scheduled:
 *   - Starts automatically on application startup via @PostConstruct
 *   - Continuously long-polls SQS (20s wait time) for near-instant processing
 *   - No artificial delay between polls (unlike @Scheduled fixedDelay)
 *   - Graceful shutdown via @PreDestroy
 */
@Component
public class SqsOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderConsumer.class);

    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessTicketOrderUseCase processOrderUseCase;
    private final String queueUrl;
    private final int maxMessages;
    private final int waitTimeSeconds;

    private Disposable pollingSubscription;

    public SqsOrderConsumer(
            SqsAsyncClient sqsClient,
            ObjectMapper objectMapper,
            ProcessTicketOrderUseCase processOrderUseCase,
            @Value("${application.sqs.order-queue-url:ticket-order-queue}") String queueUrl,
            @Value("${application.sqs.max-messages:10}") int maxMessages,
            @Value("${application.sqs.wait-time-seconds:20}") int waitTimeSeconds
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.processOrderUseCase = processOrderUseCase;
        this.queueUrl = queueUrl;
        this.maxMessages = maxMessages;
        this.waitTimeSeconds = waitTimeSeconds;
    }

    /**
     * Starts the reactive polling loop on application startup.
     * Replaces @Scheduled with a continuous reactive stream.
     */
    @PostConstruct
    public void startPolling() {
        log.info("Starting reactive SQS consumer for queue: {} (long-poll={}s, maxMessages={})",
                queueUrl, waitTimeSeconds, maxMessages);

        pollingSubscription = Flux.defer(this::pollOnce)
                .repeat()
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn(
                                "SQS polling error, retrying (attempt {}): {}",
                                signal.totalRetries() + 1, signal.failure().getMessage())))
                .subscribe(
                        count -> { /* processed silently */ },
                        error -> log.error("Fatal error in SQS polling loop", error)
                );
    }

    /**
     * Gracefully stops the polling loop on application shutdown.
     */
    @PreDestroy
    public void stopPolling() {
        log.info("Stopping reactive SQS consumer...");
        if (pollingSubscription != null && !pollingSubscription.isDisposed()) {
            pollingSubscription.dispose();
        }
    }

    /**
     * Performs a single poll iteration:
     * 1. Long-polls SQS for messages (blocks up to waitTimeSeconds)
     * 2. Processes each message reactively
     * 3. Returns the number of messages processed
     */
    Mono<Integer> pollOnce() {
        return Mono.fromFuture(() -> {
                    ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                            .queueUrl(getQueueUrl())
                            .maxNumberOfMessages(maxMessages)
                            .waitTimeSeconds(waitTimeSeconds)
                            .build();
                    return sqsClient.receiveMessage(request);
                })
                .flatMap(response -> {
                    List<Message> messages = response.messages();
                    if (messages.isEmpty()) {
                        return Mono.just(0);
                    }

                    log.info("Received {} messages from SQS queue", messages.size());

                    return Flux.fromIterable(messages)
                            .flatMap(this::processMessage)
                            .then(Mono.just(messages.size()));
                })
                .onErrorResume(error -> {
                    log.error("Error during SQS poll", error);
                    return Mono.just(0);
                });
    }

    /**
     * Processes a single SQS message.
     * Handles SNS-wrapped messages (when message comes via SNS → SQS subscription).
     */
    private Mono<Void> processMessage(Message message) {
        return Mono.fromCallable(() -> {
                    String messageBody = message.body();
                    log.debug("Received message body: {}", messageBody);

                    if (messageBody == null || messageBody.trim().isEmpty()) {
                        log.error("Received empty message body, deleting message");
                        return null;
                    }

                    // Handle SNS-wrapped messages: SNS wraps the original message in an envelope
                    String actualBody = unwrapSnsMessage(messageBody);
                    return objectMapper.readValue(actualBody, OrderMessage.class);
                })
                .flatMap(orderMessage -> {
                    if (orderMessage == null) {
                        return deleteMessage(message);
                    }

                    log.info("Processing order message: orderId={}, ticketType={}",
                            orderMessage.orderId(), orderMessage.ticketType());

                    return processOrderUseCase.execute(orderMessage.orderId())
                            .then(deleteMessage(message))
                            .doOnSuccess(v -> log.info("Order processed successfully: orderId={}",
                                    orderMessage.orderId()))
                            .onErrorResume(error -> {
                                log.error("Error processing order: orderId={}",
                                        orderMessage.orderId(), error);
                                // Message will become visible again after visibility timeout
                                return Mono.empty();
                            });
                })
                .onErrorResume(com.fasterxml.jackson.databind.exc.ValueInstantiationException.class, error -> {
                    log.error("Invalid message format - cannot deserialize OrderMessage. Body: {}, Error: {}",
                            message.body(), error.getMessage());
                    return deleteMessage(message);
                })
                .onErrorResume(error -> {
                    log.error("Error parsing message: receiptHandle={}, body={}",
                            message.receiptHandle(), message.body(), error);
                    return deleteMessage(message);
                });
    }

    /**
     * Unwraps SNS envelope from SQS messages.
     * When SNS publishes to SQS, the message body is wrapped in an SNS envelope
     * containing fields like "Type", "MessageId", "TopicArn", "Message", etc.
     * The actual order message is inside the "Message" field.
     */
    private String unwrapSnsMessage(String messageBody) {
        try {
            var node = objectMapper.readTree(messageBody);
            // Check if this is an SNS envelope (has "Type" and "Message" fields)
            if (node.has("Type") && node.has("Message")) {
                String innerMessage = node.get("Message").asText();
                log.debug("Unwrapped SNS envelope, inner message: {}", innerMessage);
                return innerMessage;
            }
        } catch (Exception e) {
            // Not a JSON structure or not an SNS envelope - use as-is
            log.debug("Message is not SNS-wrapped, using as-is");
        }
        return messageBody;
    }

    /**
     * Deletes a processed message from the SQS queue.
     */
    private Mono<Void> deleteMessage(Message message) {
        return Mono.fromFuture(() -> {
                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(getQueueUrl())
                            .receiptHandle(message.receiptHandle())
                            .build();
                    return sqsClient.deleteMessage(deleteRequest);
                })
                .doOnSuccess(r -> log.debug("Message deleted: receiptHandle={}", message.receiptHandle()))
                .doOnError(error -> log.error("Error deleting message", error))
                .then();
    }

    private String getQueueUrl() {
        if (queueUrl.startsWith("http")) {
            return queueUrl;
        }
        // Try to get actual queue URL from SQS
        try {
            var getQueueUrlRequest = software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.builder()
                    .queueName(queueUrl)
                    .build();
            var future = sqsClient.getQueueUrl(getQueueUrlRequest);
            String url = future.join().queueUrl();
            log.debug("Retrieved queue URL from SQS: {}", url);
            return url;
        } catch (Exception e) {
            // Fallback: construct URL for LocalStack
            String localstackHost = System.getenv().getOrDefault("LOCALSTACK_HOSTNAME", "localstack");
            String constructedUrl = "http://%s:4566/000000000000/%s".formatted(localstackHost, queueUrl);
            log.debug("Using constructed queue URL: {}", constructedUrl);
            return constructedUrl;
        }
    }
}
