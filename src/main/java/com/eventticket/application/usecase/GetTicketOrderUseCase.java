package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for retrieving a ticket order.
 * Query use case following CQRS pattern.
 * Tickets are loaded separately from TicketItems table.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class GetTicketOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetTicketOrderUseCase.class);

    private final TicketOrderRepository orderRepository;
    private final TicketItemRepository ticketItemRepository;

    public GetTicketOrderUseCase(
            TicketOrderRepository orderRepository,
            TicketItemRepository ticketItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.ticketItemRepository = ticketItemRepository;
    }

    /**
     * Executes the get order use case.
     * Loads order and its tickets separately from TicketItems table.
     *
     * @param orderId Order identifier
     * @return Order response with tickets
     */
    public Mono<OrderResponse> execute(String orderId) {
        log.debug("Retrieving order: {}", orderId);
        
        OrderId id = OrderId.of(orderId);
        
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order -> 
                    // Load tickets separately from TicketItems table
                    ticketItemRepository.findByOrderId(id)
                            .collectList()
                            .map(tickets -> OrderResponse.fromDomain(order, tickets))
                )
                .doOnSuccess(response -> log.debug("Order retrieved successfully: {}", orderId))
                .doOnError(error -> log.error("Error retrieving order: {}", orderId, error));
    }
}
