package com.eventticket.application.usecase;

import com.eventticket.application.dto.ConfirmOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.CustomerInfo;
import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.repository.CustomerInfoRepository;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
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
    private final TicketItemRepository ticketItemRepository;
    private final CustomerInfoRepository customerInfoRepository;

    public ConfirmTicketOrderUseCase(
            TicketOrderRepository orderRepository,
            TicketItemRepository ticketItemRepository,
            CustomerInfoRepository customerInfoRepository
    ) {
        this.orderRepository = orderRepository;
        this.ticketItemRepository = ticketItemRepository;
        this.customerInfoRepository = customerInfoRepository;
    }

    /**
     * Executes the confirm order use case.
     * Changes order status from RESERVED to PENDING_CONFIRMATION.
     * Saves customer payment information.
     *
     * @param orderId Order identifier
     * @param request Customer payment information
     * @return Confirmed order response
     */
    public Mono<OrderResponse> execute(String orderId, ConfirmOrderRequest request) {
        log.info("Confirming order: {} with customer info", orderId);
        
        OrderId id = OrderId.of(orderId);
        
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order -> {
                    if (order.getStatus() != OrderStatus.RESERVED) {
                        return Mono.error(new IllegalStateException(
                                "Order must be in RESERVED status to be confirmed. " +
                                "Current status: " + order.getStatus().name()));
                    }
                    
                    // Save customer information
                    CustomerInfo customerInfo = CustomerInfo.create(
                            order.getCustomerId(),
                            order.getOrderId(),
                            request.customerName(),
                            request.email(),
                            request.phoneNumber(),
                            request.address(),
                            request.city(),
                            request.country(),
                            request.paymentMethod()
                    );
                    
                    return customerInfoRepository.save(customerInfo)
                            .then(Mono.defer(() -> {
                                // Update tickets to PENDING_CONFIRMATION status
                                return ticketItemRepository.findByOrderId(order.getOrderId())
                                        .map(ticket -> ticket.confirmPayment(order.getCustomerId().value()))
                                        .collectList()
                                        .flatMap(confirmedTickets -> 
                                            ticketItemRepository.saveAll(confirmedTickets)
                                                .then(Mono.just(order))
                                        );
                            }));
                })
                .flatMap(order -> {
                    // Change status from RESERVED to PENDING_CONFIRMATION
                    TicketOrder confirmedOrder = order.confirm();
                    return orderRepository.save(confirmedOrder);
                })
                .flatMap(order -> 
                    // Load tickets from TicketItems table for response
                    ticketItemRepository.findByOrderId(order.getOrderId())
                            .collectList()
                            .map(tickets -> OrderResponse.fromDomain(order, tickets))
                )
                .doOnSuccess(response -> log.info("Order confirmed: {} (RESERVED -> PENDING_CONFIRMATION) with customer info", response.orderId()))
                .doOnError(error -> log.error("Error confirming order: {}", orderId, error));
    }
}
