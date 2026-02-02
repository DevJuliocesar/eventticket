package com.eventticket.domain.repository;

import com.eventticket.domain.model.CustomerInfo;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface for CustomerInfo aggregate.
 * Follows the Repository pattern from DDD.
 */
public interface CustomerInfoRepository {

    /**
     * Saves customer information.
     *
     * @param customerInfo Customer information to save
     * @return Saved customer information
     */
    Mono<CustomerInfo> save(CustomerInfo customerInfo);

    /**
     * Finds customer information by order identifier.
     *
     * @param orderId Order identifier
     * @return Customer information if found, empty Mono otherwise
     */
    Mono<CustomerInfo> findByOrderId(OrderId orderId);

    /**
     * Finds all customer information for a given customer.
     *
     * @param customerId Customer identifier
     * @return Flux of customer information
     */
    Flux<CustomerInfo> findByCustomerId(CustomerId customerId);

    /**
     * Deletes customer information by order identifier.
     *
     * @param orderId Order identifier
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteByOrderId(OrderId orderId);
}
