package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of TicketOrderRepository.
 * TODO: Replace with DynamoDB implementation.
 */
@Repository
public class DynamoDBTicketOrderRepository implements TicketOrderRepository {

    private final ConcurrentMap<String, TicketOrder> orders = new ConcurrentHashMap<>();

    @Override
    public Mono<TicketOrder> save(TicketOrder order) {
        orders.put(order.getOrderId().value(), order);
        return Mono.just(order);
    }

    @Override
    public Mono<TicketOrder> findById(OrderId orderId) {
        TicketOrder order = orders.get(orderId.value());
        return order != null ? Mono.just(order) : Mono.empty();
    }

    @Override
    public Flux<TicketOrder> findByCustomerId(CustomerId customerId) {
        return Flux.fromIterable(orders.values())
                .filter(order -> order.getCustomerId().equals(customerId));
    }

    @Override
    public Flux<TicketOrder> findByStatus(OrderStatus status) {
        return Flux.fromIterable(orders.values())
                .filter(order -> order.getStatus() == status);
    }

    @Override
    public Mono<Void> deleteById(OrderId orderId) {
        orders.remove(orderId.value());
        return Mono.empty();
    }
}
