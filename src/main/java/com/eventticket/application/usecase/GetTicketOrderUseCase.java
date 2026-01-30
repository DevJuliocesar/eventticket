package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for retrieving a ticket order.
 * Query use case following CQRS pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetTicketOrderUseCase {

    private final TicketOrderRepository orderRepository;

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
