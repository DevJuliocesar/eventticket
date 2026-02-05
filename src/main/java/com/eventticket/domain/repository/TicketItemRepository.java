package com.eventticket.domain.repository;

import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.ReservationId;
import com.eventticket.domain.valueobject.TicketId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface for TicketItem aggregate.
 * Follows the Repository pattern from DDD.
 */
public interface TicketItemRepository {

    /**
     * Saves a ticket item.
     *
     * @param ticketItem Ticket item to save
     * @return Saved ticket item
     */
    Mono<TicketItem> save(TicketItem ticketItem);

    /**
     * Saves multiple ticket items.
     *
     * @param ticketItems List of ticket items to save
     * @return Flux of saved ticket items
     */
    Flux<TicketItem> saveAll(java.util.List<TicketItem> ticketItems);

    /**
     * Finds a ticket item by its identifier.
     *
     * @param ticketId Ticket identifier
     * @return Ticket item if found, empty Mono otherwise
     */
    Mono<TicketItem> findById(TicketId ticketId);

    /**
     * Finds all ticket items for a given order.
     *
     * @param orderId Order identifier
     * @return Flux of ticket items
     */
    Flux<TicketItem> findByOrderId(OrderId orderId);

    /**
     * Finds all ticket items for a given reservation.
     *
     * @param reservationId Reservation identifier
     * @return Flux of ticket items
     */
    Flux<TicketItem> findByReservationId(ReservationId reservationId);

    /**
     * Deletes a ticket item by its identifier.
     *
     * @param ticketId Ticket identifier
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteById(TicketId ticketId);

    /**
     * Deletes all ticket items for a given order.
     *
     * @param orderId Order identifier
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteByOrderId(OrderId orderId);

    /**
     * Finds all ticket items for a given event and ticket type that have assigned seat numbers.
     * Used to check occupied seats for seat assignment.
     *
     * @param eventId Event identifier
     * @param ticketType Ticket type
     * @return Flux of ticket items with seat numbers
     */
    Flux<TicketItem> findByEventIdAndTicketTypeWithSeatNumber(
            com.eventticket.domain.valueobject.EventId eventId,
            String ticketType
    );

    /**
     * Assigns seat numbers to tickets using DynamoDB transactions.
     * This ensures atomic assignment and prevents duplicate seat numbers.
     *
     * @param tickets List of tickets to assign seats to
     * @param eventId Event identifier
     * @param ticketType Ticket type
     * @param seatNumbers List of seat numbers to assign (must match tickets size)
     * @param targetStatus Target status for the tickets (e.g., SOLD or COMPLIMENTARY)
     * @param allowedSourceStatuses Allowed current statuses for the tickets
     * @return Mono that completes when all seats are assigned atomically
     */
    Mono<Void> assignSeatsAtomically(
            java.util.List<TicketItem> tickets,
            com.eventticket.domain.valueobject.EventId eventId,
            String ticketType,
            java.util.List<String> seatNumbers,
            com.eventticket.domain.model.TicketStatus targetStatus,
            java.util.List<com.eventticket.domain.model.TicketStatus> allowedSourceStatuses
    );
}
