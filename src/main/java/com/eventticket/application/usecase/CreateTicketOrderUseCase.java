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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use case for creating a ticket order.
 * Implements the Command pattern and orchestrates the order creation flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateTicketOrderUseCase {

    private final TicketOrderRepository orderRepository;
    private final TicketInventoryRepository inventoryRepository;
    private final TicketReservationRepository reservationRepository;

    /**
     * Executes the create order use case.
     *
     * @param request Order creation request
     * @return Created order response
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
                .map(OrderResponse::fromDomain)
                .doOnSuccess(response -> log.info("Order created successfully: {}", response.orderId()))
                .doOnError(error -> log.error("Error creating order", error));
    }

    private Mono<TicketInventory> checkInventoryAvailability(
            EventId eventId,
            String ticketType,
            int quantity
    ) {
        return inventoryRepository.findByEventIdAndTicketType(eventId, ticketType)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Inventory not found for event: " + eventId + " and ticket type: " + ticketType)))
                .filter(inventory -> inventory.isAvailable(quantity))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Insufficient inventory. Available: " + quantity)));
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
        return java.util.stream.IntStream.range(0, quantity)
                .mapToObj(i -> TicketItem.create(
                        ticketType,
                        generateSeatNumber(i),
                        price
                ))
                .toList();
    }

    private Mono<TicketReservation> createReservation(
            TicketOrder order,
            String ticketType,
            int quantity
    ) {
        TicketReservation reservation = TicketReservation.create(
                order.getOrderId(),
                order.getEventId(),
                ticketType,
                quantity
        );
        
        return reservationRepository.save(reservation);
    }

    private String generateSeatNumber(int index) {
        char row = (char) ('A' + (index / 10));
        int seat = (index % 10) + 1;
        return row + "-" + seat;
    }
}
