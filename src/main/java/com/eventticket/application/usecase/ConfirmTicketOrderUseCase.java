package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for confirming a ticket order.
 * Follows the Single Responsibility Principle.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class ConfirmTicketOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmTicketOrderUseCase.class);

    private final TicketOrderRepository orderRepository;
    private final TicketReservationRepository reservationRepository;

    public ConfirmTicketOrderUseCase(
            TicketOrderRepository orderRepository,
            TicketReservationRepository reservationRepository
    ) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
    }

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
