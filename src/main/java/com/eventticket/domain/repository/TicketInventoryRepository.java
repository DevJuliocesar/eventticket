package com.eventticket.domain.repository;

import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.valueobject.EventId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface for TicketInventory aggregate.
 */
public interface TicketInventoryRepository {

    /**
     * Saves ticket inventory.
     *
     * @param inventory Inventory to save
     * @return Saved inventory
     */
    Mono<TicketInventory> save(TicketInventory inventory);

    /**
     * Finds inventory by event and ticket type.
     *
     * @param eventId Event identifier
     * @param ticketType Ticket type
     * @return Inventory if found, empty Mono otherwise
     */
    Mono<TicketInventory> findByEventIdAndTicketType(EventId eventId, String ticketType);

    /**
     * Finds all inventory for an event.
     *
     * @param eventId Event identifier
     * @return Flux of inventory items
     */
    Flux<TicketInventory> findByEventId(EventId eventId);

    /**
     * Finds inventory for an event with pagination.
     *
     * @param eventId Event identifier
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return Flux of inventory items for the requested page
     */
    Flux<TicketInventory> findByEventId(EventId eventId, int page, int pageSize);

    /**
     * Counts total number of inventory items for an event.
     *
     * @param eventId Event identifier
     * @return Total count of inventory items
     */
    Mono<Long> countByEventId(EventId eventId);

    /**
     * Updates inventory with optimistic locking.
     *
     * @param inventory Inventory to update
     * @return Updated inventory
     */
    Mono<TicketInventory> updateWithOptimisticLock(TicketInventory inventory);
}
