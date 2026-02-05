package com.eventticket.infrastructure.messaging;

import reactor.core.publisher.Mono;

/**
 * Port interface for publishing order messages.
 * Decouples use cases from the specific messaging implementation (SQS, SNS, etc).
 * Follows the Dependency Inversion Principle (DIP).
 */
public interface OrderMessagePublisher {

    /**
     * Publishes an order message for asynchronous processing.
     *
     * @param message Order message to publish
     * @return Mono that completes when message is sent
     */
    Mono<Void> publishOrder(OrderMessage message);
}
