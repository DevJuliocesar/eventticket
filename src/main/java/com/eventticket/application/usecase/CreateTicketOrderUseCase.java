package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.infrastructure.config.ReservationProperties;
import com.eventticket.infrastructure.messaging.OrderMessage;
import com.eventticket.infrastructure.messaging.OrderMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Use case for creating a ticket order.
 * Implements the Command pattern and orchestrates the order creation flow.
 * Functional Requirement #3: Asynchronous order processing.
 * Creates order, reserves tickets, and publishes to SNS Topic for async processing.
 * SNS fans out the message to subscribed SQS queues (SNS + SQS pattern).
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class CreateTicketOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTicketOrderUseCase.class);

    private final TicketOrderRepository orderRepository;
    private final TicketInventoryRepository inventoryRepository;
    private final TicketItemRepository ticketItemRepository;
    private final TicketReservationRepository reservationRepository;
    private final OrderMessagePublisher orderMessagePublisher;
    private final ReservationProperties reservationProperties;

    public CreateTicketOrderUseCase(
            TicketOrderRepository orderRepository,
            TicketInventoryRepository inventoryRepository,
            TicketItemRepository ticketItemRepository,
            TicketReservationRepository reservationRepository,
            OrderMessagePublisher orderMessagePublisher,
            ReservationProperties reservationProperties
    ) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.ticketItemRepository = ticketItemRepository;
        this.reservationRepository = reservationRepository;
        this.orderMessagePublisher = orderMessagePublisher;
        this.reservationProperties = reservationProperties;
    }

    /**
     * Executes the create order use case.
     * Functional Requirement #3: Returns orderId immediately and enqueues for async processing.
     *
     * @param request Order creation request
     * @return Created order response (returns immediately)
     */
    public Mono<OrderResponse> execute(CreateOrderRequest request) {
        log.info("Creating ticket order for customer: {}", request.customerId());
        
        CustomerId customerId = CustomerId.of(request.customerId());
        EventId eventId = EventId.of(request.eventId());

        return checkInventoryAvailability(eventId, request.ticketType(), request.quantity())
                .flatMap(inventory -> reserveTickets(inventory, request.quantity()))
                .flatMap(inventory -> createOrder(customerId, eventId, request, inventory))
                .flatMap(order -> createReservation(order, request.ticketType(), request.quantity())
                        .flatMap(reservation -> {
                            // Save tickets to TicketItems table with both orderId and reservationId
                            List<TicketItem> ticketsWithIds = order.getTickets().stream()
                                    .map(ticket -> ticket.withOrderId(order.getOrderId())
                                            .withReservationId(reservation.getReservationId()))
                                    .toList();
                            return ticketItemRepository.saveAll(ticketsWithIds)
                                    .then(Mono.just(order));
                        }))
                .flatMap(order -> {
                    // Publish order to SNS Topic for asynchronous processing (fan-out to SQS)
                    OrderMessage message = OrderMessage.of(
                            order.getOrderId(),
                            order.getEventId().value(),
                            order.getCustomerId().value(),
                            request.ticketType(),
                            request.quantity()
                    );
                    return orderMessagePublisher.publishOrder(message)
                            .thenReturn(order);
                })
                .flatMap(order -> 
                    // Load tickets from TicketItems table for response
                    ticketItemRepository.findByOrderId(order.getOrderId())
                            .collectList()
                            .map(tickets -> OrderResponse.fromDomain(order, tickets))
                )
                .doOnSuccess(response -> log.info(
                        "Order created and enqueued successfully: {}", response.orderId()))
                .doOnError(error -> log.error("Error creating order", error));
    }

    private Mono<TicketInventory> checkInventoryAvailability(
            EventId eventId,
            String ticketType,
            int quantity
    ) {
        return inventoryRepository.findByEventIdAndTicketType(eventId, ticketType)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Inventory not found for event: %s and ticket type: %s".formatted(eventId, ticketType))))
                .filter(inventory -> inventory.isAvailable(quantity))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Insufficient inventory. Requested: %d".formatted(quantity))));
    }

    private Mono<TicketInventory> reserveTickets(TicketInventory inventory, int quantity) {
        TicketInventory reservedInventory = inventory.reserve(quantity);
        return inventoryRepository.updateWithOptimisticLock(reservedInventory);
    }

    private Mono<TicketOrder> createOrder(
            CustomerId customerId,
            EventId eventId,
            CreateOrderRequest request,
            TicketInventory inventory
    ) {
        List<TicketItem> items = createTicketItems(request.ticketType(), request.quantity(), inventory.getPrice());
        
        TicketOrder order = TicketOrder.create(
                customerId,
                eventId,
                request.eventName(),
                items
        );
        
        return orderRepository.save(order);
    }

    private List<TicketItem> createTicketItems(String ticketType, int quantity, Money price) {
        // Create tickets without seat numbers - they will be assigned when SOLD or COMPLIMENTARY
        return IntStream.range(0, quantity)
                .mapToObj(i -> TicketItem.create(ticketType, price))
                .toList();
    }

    private Mono<TicketReservation> createReservation(
            TicketOrder order,
            String ticketType,
            int quantity
    ) {
        int timeoutMinutes = reservationProperties.getTimeoutMinutes();
        TicketReservation reservation = TicketReservation.create(
                order.getOrderId(),
                order.getEventId(),
                ticketType,
                quantity,
                timeoutMinutes
        );
        
        log.debug("Created reservation with timeout: {} minutes", timeoutMinutes);
        return reservationRepository.save(reservation);
    }
}
