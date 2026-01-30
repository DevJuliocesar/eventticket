package com.eventticket.domain.repository;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface for TicketOrder aggregate.
 * Follows the Repository pattern from DDD.
 */
public interface TicketOrderRepository {

    /**
     * Saves a ticket order.
     *
     * @param order Order to save
     * @return Saved order
     */
    Mono<TicketOrder> save(TicketOrder order);

    /**
     * Finds an order by its identifier.
     *
     * @param orderId Order identifier
     * @return Order if found, empty Mono otherwise
     */
    Mono<TicketOrder> findById(OrderId orderId);

    /**
     * Finds orders by customer identifier.
     *
     * @param customerId Customer identifier
     * @return Flux of orders
     */
    Flux<TicketOrder> findByCustomerId(CustomerId customerId);

    /**
     * Finds orders by status.
     *
     * @param status Order status
     * @return Flux of orders
     */
    Flux<TicketOrder> findByStatus(OrderStatus status);

    /**
     * Deletes an order by its identifier.
     *
     * @param orderId Order identifier
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteById(OrderId orderId);
}
