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
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case for marking an order as sold (payment completed).
 * Updates both the order status and the ticket inventory.
 * Assigns unique seat numbers when marking as sold.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class MarkOrderAsSoldUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkOrderAsSoldUseCase.class);

    private final TicketOrderRepository orderRepository;
    private final TicketInventoryRepository inventoryRepository;
    private final TicketItemRepository ticketItemRepository;

    public MarkOrderAsSoldUseCase(
            TicketOrderRepository orderRepository,
            TicketInventoryRepository inventoryRepository,
            TicketItemRepository ticketItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.ticketItemRepository = ticketItemRepository;
    }

    /**
     * Executes the mark order as sold use case.
     * Updates order status to SOLD, assigns unique seat numbers, and confirms reservation in inventory.
     * Implements retry mechanism to ensure seat uniqueness in concurrent scenarios.
     *
     * @param orderId Order identifier
     * @return Sold order response
     */
    public Mono<OrderResponse> execute(String orderId) {
        log.info("Marking order as sold: {}", orderId);
        
        OrderId id = OrderId.of(orderId);
        
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .filter(order -> order.getStatus() == OrderStatus.PENDING_CONFIRMATION)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Order must be in PENDING_CONFIRMATION status to be marked as sold: " + orderId)))
                .flatMap(order -> markOrderAsSoldWithRetry(order, 0, 3))
                .flatMap(order -> 
                    // Load tickets from TicketItems table for response
                    ticketItemRepository.findByOrderId(order.getOrderId())
                            .collectList()
                            .map(tickets -> OrderResponse.fromDomain(order, tickets))
                )
                .doOnSuccess(response -> log.info("Order marked as sold: {}", response.orderId()))
                .doOnError(error -> log.error("Error marking order as sold: {}", orderId, error));
    }

    /**
     * Marks order as sold with retry mechanism to ensure seat uniqueness.
     * Retries up to maxRetries times if seat conflict is detected.
     */
    private Mono<TicketOrder> markOrderAsSoldWithRetry(TicketOrder order, int attempt, int maxRetries) {
        if (attempt >= maxRetries) {
            return Mono.error(new IllegalStateException(
                    "Failed to assign unique seats after %d attempts for order: %s"
                            .formatted(maxRetries, order.getOrderId())));
        }

        // Load tickets from TicketItems table
        return ticketItemRepository.findByOrderId(order.getOrderId())
                .collectList()
                .flatMap(tickets -> {
                    if (tickets.isEmpty()) {
                        return Mono.error(new IllegalStateException(
                                "No tickets found for order: " + order.getOrderId()));
                    }
                    
                    String ticketType = tickets.get(0).getTicketType();
        
                    return findAvailableSeatNumbers(order.getEventId(), ticketType, tickets.size())
                            .flatMap(seatNumbers -> {
                                // Verify seat uniqueness one more time before saving (double-check)
                                return verifySeatUniqueness(order.getEventId(), ticketType, seatNumbers)
                                        .flatMap(areUnique -> {
                                            if (!areUnique) {
                                                log.warn("Seat conflict detected for order {} on attempt {}. Retrying...", 
                                                        order.getOrderId(), attempt + 1);
                                                // Retry with fresh seat lookup
                                                return markOrderAsSoldWithRetry(order, attempt + 1, maxRetries);
                                            }
                                            
                                            // Use DynamoDB transaction to assign seats atomically
                                            // This ensures no duplicate seats can be assigned
                                            return ticketItemRepository.assignSeatsAtomically(
                                                            tickets,
                                                            order.getEventId(),
                                                            ticketType,
                                                            seatNumbers,
                                                            TicketStatus.SOLD,
                                                            List.of(TicketStatus.PENDING_CONFIRMATION)
                                                    )
                                                    .then(Mono.defer(() -> {
                                                        // After successful transaction, update tickets and order
                                                        List<TicketItem> updatedTickets = new ArrayList<>();
                                                        for (int i = 0; i < tickets.size(); i++) {
                                                            TicketItem ticket = tickets.get(i);
                                                            TicketItem soldTicket = ticket.markAsSold(
                                                                    order.getCustomerId().value(), 
                                                                    seatNumbers.get(i)
                                                            );
                                                            updatedTickets.add(soldTicket);
                                                        }
                                                        
                                                        // Mark order as sold with updated tickets
                                                        TicketOrder soldOrder = order.markAsSold(updatedTickets);
                                                        return orderRepository.save(soldOrder);
                                                    }))
                                                    .flatMap(updatedOrder -> updateInventory(updatedOrder).thenReturn(updatedOrder))
                                                    .onErrorResume(IllegalStateException.class, e -> {
                                                        // Transaction failed - seats were already reserved
                                                        log.warn("Seat assignment transaction failed for order {} on attempt {}. Retrying...", 
                                                                order.getOrderId(), attempt + 1);
                                                        return markOrderAsSoldWithRetry(order, attempt + 1, maxRetries);
                                                    });
                                        });
                            });
                });
    }

    /**
     * Verifies that the seat numbers are unique before assignment.
     * This is a double-check to catch race conditions.
     */
    private Mono<Boolean> verifySeatUniqueness(
            com.eventticket.domain.valueobject.EventId eventId,
            String ticketType,
            List<String> seatNumbers
    ) {
        // Query tickets directly from TicketItems table instead of from orders
        return ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, ticketType)
                .filter(ticket -> {
                    TicketStatus status = ticket.getStatus();
                    return (status == TicketStatus.SOLD || status == TicketStatus.COMPLIMENTARY)
                            && ticket.getTicketType().equals(ticketType)
                            && ticket.getSeatNumber() != null;
                })
                .map(TicketItem::getSeatNumber)
                .collectList()
                .map(occupiedSeats -> {
                    // Check if any of our candidate seats are already occupied
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
     * Verifies seat uniqueness after saving the order.
     * This catches cases where another order was saved concurrently with the same seats.
     */
    private Mono<Boolean> verifySeatUniquenessAfterSave(TicketOrder savedOrder) {
        String ticketType = savedOrder.getTickets().get(0).getTicketType();
        List<String> assignedSeats = savedOrder.getTickets().stream()
                .map(TicketItem::getSeatNumber)
                .filter(seat -> seat != null)
                .toList();
        
        return orderRepository.findByEventId(savedOrder.getEventId())
                .filter(order -> !order.getOrderId().equals(savedOrder.getOrderId())) // Exclude current order
                .flatMap(order -> Flux.fromIterable(order.getTickets()))
                .filter(ticket -> {
                    TicketStatus status = ticket.getStatus();
                    return (status == TicketStatus.SOLD || status == TicketStatus.COMPLIMENTARY)
                            && ticket.getTicketType().equals(ticketType)
                            && ticket.getSeatNumber() != null;
                })
                .map(TicketItem::getSeatNumber)
                .collectList()
                .map(occupiedSeats -> {
                    // Check if any of our assigned seats are duplicated in other orders
                    for (String seat : assignedSeats) {
                        if (occupiedSeats.contains(seat)) {
                            log.error("CRITICAL: Seat {} is duplicated! Order {} has conflict with another order", 
                                    seat, savedOrder.getOrderId());
                            return false;
                        }
                    }
                    return true;
                });
    }

    /**
     * Attempts to correct a seat conflict by reassigning seats.
     * This is a fallback mechanism for critical race conditions.
     * Note: Since SOLD is a final state, this should rarely happen if verification works correctly.
     */
    private Mono<TicketOrder> correctSeatConflict(TicketOrder order, int attempt, int maxRetries) {
        if (attempt >= maxRetries) {
            log.error("CRITICAL: Could not correct seat conflict for order {} after {} attempts. " +
                    "Manual intervention required! Order ID: {}", order.getOrderId(), maxRetries, order.getOrderId().value());
            // Return the order with a warning - this should trigger an alert in production
            return Mono.just(order);
        }

        log.warn("Attempting to correct seat conflict for order {} (attempt {}/{})", 
                order.getOrderId(), attempt + 1, maxRetries);
        
        // For now, we log the error but don't modify SOLD orders
        // In production, this would trigger an alert and manual review
        // The verification before save should prevent this in most cases
        return Mono.just(order)
                .doOnSuccess(o -> log.error(
                        "Seat conflict detected in SOLD order {}. Seats may be duplicated. " +
                        "This requires manual review. Assigned seats: {}", 
                        o.getOrderId(),
                        o.getTickets().stream()
                                .map(TicketItem::getSeatNumber)
                                .toList()));
    }

    /**
     * Finds available seat numbers for tickets.
     * Checks occupied seats from SOLD and COMPLIMENTARY tickets only.
     * 
     * Algorithm:
     * 1. Query all tickets with seat numbers for this event and ticket type
     * 2. Generate seat numbers sequentially (A-1, A-2, ..., A-10, B-1, ...)
     * 3. Skip occupied seats and assign available ones
     * 
     * Seat number format: "{ROW}-{NUMBER}"
     * - ROW: A, B, C, ... (one letter per 10 seats)
     * - NUMBER: 1-10 (seat number within the row)
     * Examples: A-1, A-2, ..., A-10, B-1, B-2, ...
     */
    private Mono<List<String>> findAvailableSeatNumbers(
            com.eventticket.domain.valueobject.EventId eventId,
            String ticketType,
            int quantity
    ) {
        // First, get all occupied seats from the ticket items table
        // Note: We need to filter by eventId, but TicketItem doesn't have eventId directly
        // For now, we'll get tickets by ticketType and filter by order's eventId
        return ticketItemRepository.findByEventIdAndTicketTypeWithSeatNumber(eventId, ticketType)
                .filter(ticket -> {
                    // Additional filter: verify ticket belongs to an order for this event
                    // Since TicketItem doesn't have eventId, we check through orders
                    return ticket.getOrderId() != null;
                })
                .map(TicketItem::getSeatNumber)
                .collectList()
                .flatMap(occupiedSeats -> {
                    // Filter occupied seats that belong to this event
                    // We need to verify through orders, but for now we'll use all occupied seats
                    // TODO: Add eventId to TicketItem for better performance
                    return orderRepository.findByEventId(eventId)
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
                                // Combine both sources of occupied seats
                                List<String> combinedOccupied = new ArrayList<>(occupiedSeats);
                                combinedOccupied.addAll(allOccupiedSeats);
                                
                                // Generate available seats
                                List<String> availableSeats = new ArrayList<>();
                                int seatIndex = 0;
                                int maxAttempts = 10000; // Safety limit
                                
                                while (availableSeats.size() < quantity && seatIndex < maxAttempts) {
                                    String candidateSeat = generateSeatNumber(seatIndex);
                                    if (!combinedOccupied.contains(candidateSeat)) {
                                        availableSeats.add(candidateSeat);
                                        log.debug("Assigned seat {} for event {} (attempt {})", 
                                                candidateSeat, eventId, seatIndex);
                                    }
                                    seatIndex++;
                                }
                                
                                if (availableSeats.size() < quantity) {
                                    throw new IllegalStateException(
                                            "Could not find %d available seats for event %s and ticket type %s. " +
                                            "Found %d available seats after checking %d candidates. " +
                                            "Occupied seats: %d"
                                                    .formatted(quantity, eventId, ticketType, 
                                                            availableSeats.size(), seatIndex, combinedOccupied.size()));
                                }
                                
                                log.info("Assigned {} unique seats for event {} and ticket type {}: {}",
                                        quantity, eventId, ticketType, availableSeats);
                                return availableSeats;
                            });
                });
    }

    /**
     * Generates a seat number from an index.
     * 
     * Formula:
     * - Row: A + (index / 10)  → A, B, C, ... (one letter per 10 seats)
     * - Seat: (index % 10) + 1  → 1-10 (seat number within row)
     * 
     * Examples:
     * - index 0  → A-1
     * - index 1  → A-2
     * - index 9  → A-10
     * - index 10 → B-1
     * - index 11 → B-2
     * - index 19 → B-10
     * - index 20 → C-1
     * 
     * This ensures sequential seat assignment without gaps.
     */
    private String generateSeatNumber(int index) {
        char row = (char) ('A' + (index / 10));
        int seat = (index % 10) + 1;
        return "%c-%d".formatted(row, seat);
    }

    private Mono<Void> updateInventory(TicketOrder order) {
        // Get ticket type from first ticket (all tickets in an order have the same type)
        if (order.getTickets().isEmpty()) {
            log.warn("Order {} has no tickets, skipping inventory update", order.getOrderId());
            return Mono.empty();
        }
        
        String ticketType = order.getTickets().get(0).getTicketType();
        int quantity = order.getTickets().size();
        
        return inventoryRepository.findByEventIdAndTicketType(order.getEventId(), ticketType)
                .flatMap(inventory -> {
                    // Confirm reservation: reduces reservedQuantity (tickets are now sold)
                    TicketInventory updatedInventory = inventory.confirmReservation(quantity);
                    return inventoryRepository.updateWithOptimisticLock(updatedInventory)
                            .doOnSuccess(inv -> log.debug(
                                    "Inventory updated for order {}: reservedQuantity reduced by {}",
                                    order.getOrderId(), quantity))
                            .then();
                })
                .doOnError(error -> log.error(
                        "Error updating inventory for order {}: ticketType={}, quantity={}",
                        order.getOrderId(), ticketType, quantity, error));
    }
}
