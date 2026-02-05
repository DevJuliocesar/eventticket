package com.eventticket.application.usecase;

import com.eventticket.application.dto.OrderResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketOrder;
import com.eventticket.domain.model.TicketStatus;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.repository.TicketItemRepository;
import com.eventticket.domain.repository.TicketOrderRepository;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case for marking an order as complimentary (free tickets).
 * Business Rule: COMPLIMENTARY is a final state, but NOT counted as revenue.
 * Can be applied from AVAILABLE, RESERVED, or PENDING_CONFIRMATION status.
 * Assigns unique seat numbers and updates inventory accordingly.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class MarkOrderAsComplimentaryUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkOrderAsComplimentaryUseCase.class);

    private final TicketOrderRepository orderRepository;
    private final TicketInventoryRepository inventoryRepository;
    private final TicketItemRepository ticketItemRepository;

    public MarkOrderAsComplimentaryUseCase(
            TicketOrderRepository orderRepository,
            TicketInventoryRepository inventoryRepository,
            TicketItemRepository ticketItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.ticketItemRepository = ticketItemRepository;
    }

    /**
     * Executes the mark order as complimentary use case.
     * Updates order status to COMPLIMENTARY, assigns unique seat numbers,
     * and adjusts inventory (releases reserved if applicable).
     *
     * @param orderId Order identifier
     * @param reason  Reason for marking as complimentary (e.g., "VIP guest", "promotional")
     * @return Complimentary order response
     */
    public Mono<OrderResponse> execute(String orderId, String reason) {
        log.info("Marking order as complimentary: {}, reason: {}", orderId, reason);

        OrderId id = OrderId.of(orderId);

        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .filter(order -> canMarkAsComplimentary(order.getStatus()))
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Order cannot be marked as complimentary from its current status: " + orderId)))
                .flatMap(order -> markAsComplimentaryWithRetry(order, reason, 0, 3))
                .flatMap(order ->
                        ticketItemRepository.findByOrderId(order.getOrderId())
                                .collectList()
                                .map(tickets -> OrderResponse.fromDomain(order, tickets))
                )
                .doOnSuccess(response -> log.info("Order marked as complimentary: {}", response.orderId()))
                .doOnError(error -> log.error("Error marking order as complimentary: {}", orderId, error));
    }

    /**
     * Checks if the order can be marked as complimentary from its current status.
     */
    private boolean canMarkAsComplimentary(OrderStatus status) {
        return status == OrderStatus.AVAILABLE
                || status == OrderStatus.RESERVED
                || status == OrderStatus.PENDING_CONFIRMATION;
    }

    /**
     * Marks order as complimentary with retry mechanism for seat assignment.
     */
    private Mono<TicketOrder> markAsComplimentaryWithRetry(
            TicketOrder order, String reason, int attempt, int maxRetries
    ) {
        if (attempt >= maxRetries) {
            return Mono.error(new IllegalStateException(
                    "Failed to assign unique seats after %d attempts for complimentary order: %s"
                            .formatted(maxRetries, order.getOrderId())));
        }

        return ticketItemRepository.findByOrderId(order.getOrderId())
                .collectList()
                .flatMap(tickets -> {
                    if (tickets.isEmpty()) {
                        return Mono.error(new IllegalStateException(
                                "No tickets found for order: " + order.getOrderId()));
                    }

                    String ticketType = tickets.get(0).getTicketType();

                    // Determine allowed source statuses based on current order status
                    List<TicketStatus> allowedStatuses = List.of(
                            TicketStatus.AVAILABLE, TicketStatus.RESERVED, TicketStatus.PENDING_CONFIRMATION
                    );

                    return findAvailableSeatNumbers(order.getEventId(), ticketType, tickets.size())
                            .flatMap(seatNumbers -> verifySeatUniqueness(order.getEventId(), ticketType, seatNumbers)
                                    .flatMap(areUnique -> {
                                        if (!areUnique) {
                                            log.warn("Seat conflict for complimentary order {} on attempt {}. Retrying...",
                                                    order.getOrderId(), attempt + 1);
                                            return markAsComplimentaryWithRetry(order, reason, attempt + 1, maxRetries);
                                        }

                                        return ticketItemRepository.assignSeatsAtomically(
                                                        tickets, order.getEventId(), ticketType, seatNumbers,
                                                        TicketStatus.COMPLIMENTARY, allowedStatuses
                                                )
                                                .then(Mono.defer(() -> {
                                                    List<TicketItem> updatedTickets = new ArrayList<>();
                                                    for (int i = 0; i < tickets.size(); i++) {
                                                        TicketItem complimentaryTicket = tickets.get(i)
                                                                .markAsComplimentary(reason, seatNumbers.get(i));
                                                        updatedTickets.add(complimentaryTicket);
                                                    }

                                                    TicketOrder complimentaryOrder = order.markAsComplimentary(updatedTickets);
                                                    return orderRepository.save(complimentaryOrder);
                                                }))
                                                .flatMap(updatedOrder ->
                                                        adjustInventory(updatedOrder, order.getStatus())
                                                                .thenReturn(updatedOrder))
                                                .onErrorResume(IllegalStateException.class, e -> {
                                                    log.warn("Seat assignment failed for complimentary order {} on attempt {}. Retrying...",
                                                            order.getOrderId(), attempt + 1);
                                                    return markAsComplimentaryWithRetry(order, reason, attempt + 1, maxRetries);
                                                });
                                    }));
                });
    }

    /**
     * Adjusts inventory based on the previous order status.
     * - If RESERVED or PENDING_CONFIRMATION: releases reserved quantity (seats are now complimentary, not sold).
     * - If AVAILABLE: no inventory adjustment needed (tickets were not reserved yet).
     */
    private Mono<Void> adjustInventory(TicketOrder order, OrderStatus previousStatus) {
        if (order.getTickets().isEmpty()) {
            return Mono.empty();
        }

        String ticketType = order.getTickets().get(0).getTicketType();
        int quantity = order.getTickets().size();

        return inventoryRepository.findByEventIdAndTicketType(order.getEventId(), ticketType)
                .flatMap(inventory -> {
                    if (previousStatus == OrderStatus.RESERVED || previousStatus == OrderStatus.PENDING_CONFIRMATION) {
                        // Tickets were reserved - confirm the reservation (moves from reserved to sold count)
                        // Even though complimentary, the seats are occupied
                        TicketInventory updatedInventory = inventory.confirmReservation(quantity);
                        return inventoryRepository.updateWithOptimisticLock(updatedInventory)
                                .doOnSuccess(inv -> log.debug(
                                        "Inventory adjusted for complimentary order {}: {} reserved tickets confirmed",
                                        order.getOrderId(), quantity))
                                .then();
                    } else {
                        // AVAILABLE: tickets haven't been reserved in inventory yet
                        // We need to reduce available count directly
                        TicketInventory reserved = inventory.reserve(quantity);
                        TicketInventory confirmed = reserved.confirmReservation(quantity);
                        return inventoryRepository.updateWithOptimisticLock(confirmed)
                                .doOnSuccess(inv -> log.debug(
                                        "Inventory adjusted for complimentary order {}: {} available tickets consumed",
                                        order.getOrderId(), quantity))
                                .then();
                    }
                })
                .doOnError(error -> log.error(
                        "Error adjusting inventory for complimentary order {}: ticketType={}, quantity={}",
                        order.getOrderId(), ticketType, quantity, error));
    }

    /**
     * Verifies that seat numbers are unique.
     */
    private Mono<Boolean> verifySeatUniqueness(EventId eventId, String ticketType, List<String> seatNumbers) {
        return ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, ticketType)
                .filter(ticket -> {
                    TicketStatus status = ticket.getStatus();
                    return (status == TicketStatus.SOLD || status == TicketStatus.COMPLIMENTARY)
                            && ticket.getSeatNumber() != null;
                })
                .map(TicketItem::getSeatNumber)
                .collectList()
                .map(occupiedSeats -> {
                    for (String seat : seatNumbers) {
                        if (occupiedSeats.contains(seat)) {
                            log.warn("Seat {} is already occupied for event {} and ticket type {}",
                                    seat, eventId, ticketType);
                            return false;
                        }
                    }
                    return true;
                });
    }

    /**
     * Finds available seat numbers for the complimentary tickets.
     */
    private Mono<List<String>> findAvailableSeatNumbers(EventId eventId, String ticketType, int quantity) {
        return ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, ticketType)
                .filter(ticket -> ticket.getOrderId() != null)
                .map(TicketItem::getSeatNumber)
                .collectList()
                .flatMap(occupiedSeats ->
                        orderRepository.findByEventId(eventId)
                                .flatMap(order -> Flux.fromIterable(order.getTickets()))
                                .filter(ticket -> {
                                    TicketStatus status = ticket.getStatus();
                                    return (status == TicketStatus.SOLD || status == TicketStatus.COMPLIMENTARY)
                                            && ticket.getTicketType().equals(ticketType)
                                            && ticket.getSeatNumber() != null;
                                })
                                .map(TicketItem::getSeatNumber)
                                .collectList()
                                .map(allOccupiedSeats -> {
                                    List<String> combinedOccupied = new ArrayList<>(occupiedSeats);
                                    combinedOccupied.addAll(allOccupiedSeats);

                                    List<String> availableSeats = new ArrayList<>();
                                    int seatIndex = 0;
                                    int maxAttempts = 10000;

                                    while (availableSeats.size() < quantity && seatIndex < maxAttempts) {
                                        String candidateSeat = generateSeatNumber(seatIndex);
                                        if (!combinedOccupied.contains(candidateSeat)) {
                                            availableSeats.add(candidateSeat);
                                        }
                                        seatIndex++;
                                    }

                                    if (availableSeats.size() < quantity) {
                                        throw new IllegalStateException(
                                                "Could not find %d available seats for complimentary order: event %s, type %s"
                                                        .formatted(quantity, eventId, ticketType));
                                    }

                                    log.info("Assigned {} seats for complimentary order: event={}, type={}, seats={}",
                                            quantity, eventId, ticketType, availableSeats);
                                    return availableSeats;
                                })
                );
    }

    /**
     * Generates a seat number from an index.
     * Format: {ROW}-{NUMBER} (e.g., A-1, A-2, ..., A-10, B-1, ...)
     */
    private String generateSeatNumber(int index) {
        char row = (char) ('A' + (index / 10));
        int seat = (index % 10) + 1;
        return "%c-%d".formatted(row, seat);
    }
}
