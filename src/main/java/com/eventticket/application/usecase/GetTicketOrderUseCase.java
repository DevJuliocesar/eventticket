package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for retrieving a ticket order.
 * Query use case following CQRS pattern.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class GetTicketOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetTicketOrderUseCase.class);

    private final TicketOrderRepository orderRepository;

    public GetTicketOrderUseCase(TicketOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Executes the get order use case.
     *
     * @param orderId Order identifier
     * @return Order response
     */
    public Mono<OrderResponse> execute(String orderId) {
        log.debug("Retrieving order: {}", orderId);
        
        return orderRepository.findById(OrderId.of(orderId))
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .map(OrderResponse::fromDomain);
    }
}
