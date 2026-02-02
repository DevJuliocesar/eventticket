package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.model.TicketReservation;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.repository.TicketReservationRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import com.eventticket.infrastructure.config.ReservationProperties;
import com.eventticket.infrastructure.messaging.OrderMessage;
import com.eventticket.infrastructure.messaging.SqsOrderPublisher;
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
 * Creates order, reserves tickets, and enqueues to SQS for async processing.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class CreateTicketOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTicketOrderUseCase.class);

    private final TicketOrderRepository orderRepository;
    private final TicketInventoryRepository inventoryRepository;
    private final TicketReservationRepository reservationRepository;
    private final SqsOrderPublisher sqsOrderPublisher;
    private final ReservationProperties reservationProperties;

    public CreateTicketOrderUseCase(
            TicketOrderRepository orderRepository,
            TicketInventoryRepository inventoryRepository,
            TicketReservationRepository reservationRepository,
            SqsOrderPublisher sqsOrderPublisher,
            ReservationProperties reservationProperties
    ) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.sqsOrderPublisher = sqsOrderPublisher;
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
                        .thenReturn(order))
                .flatMap(order -> {
                    // Enqueue order to SQS for asynchronous processing
                    OrderMessage message = OrderMessage.of(
                            order.getOrderId(),
                            order.getEventId().value(),
                            order.getCustomerId().value(),
                            request.ticketType(),
                            request.quantity()
                    );
                    return sqsOrderPublisher.publishOrder(message)
                            .thenReturn(order);
                })
                .map(OrderResponse::fromDomain)
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
        return IntStream.range(0, quantity)
                .mapToObj(i -> TicketItem.create(ticketType, generateSeatNumber(i), price))
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

    private String generateSeatNumber(int index) {
        char row = (char) ('A' + (index / 10));
        int seat = (index % 10) + 1;
        return "%c-%d".formatted(row, seat);
    }
}
