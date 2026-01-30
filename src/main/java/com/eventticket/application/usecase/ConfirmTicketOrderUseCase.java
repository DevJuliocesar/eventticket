package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for confirming a ticket order.
 * Follows the Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmTicketOrderUseCase {

    private final TicketOrderRepository orderRepository;
    private final TicketReservationRepository reservationRepository;

    /**
     * Executes the confirm order use case.
     *
     * @param orderId Order identifier
     * @return Confirmed order response
     */
    public Mono<OrderResponse> execute(String orderId) {
        log.info("Confirming order: {}", orderId);
        
        OrderId id = OrderId.of(orderId);
        
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .map(TicketOrder::confirm)
                .flatMap(orderRepository::save)
                .flatMap(order -> confirmReservations(order).thenReturn(order))
                .map(OrderResponse::fromDomain)
                .doOnSuccess(response -> log.info("Order confirmed: {}", response.orderId()))
                .doOnError(error -> log.error("Error confirming order: {}", orderId, error));
    }

    private Mono<Void> confirmReservations(TicketOrder order) {
        return reservationRepository.findByOrderId(order.getOrderId())
                .map(reservation -> reservation.confirm())
                .flatMap(reservationRepository::save)
                .then();
    }
}
