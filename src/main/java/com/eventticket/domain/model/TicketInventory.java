package com.eventticket.domain.model;

import com.eventticket.domain.exception.InsufficientInventoryException;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Domain entity representing ticket inventory for an event.
 * Implements optimistic locking through version field.
 */
@Value
@Builder
@With
public class TicketInventory {
    
    EventId eventId;
    String ticketType;
    String eventName;
    int totalQuantity;
    int availableQuantity;
    int reservedQuantity;
    Money price;
    int version;

    /**
     * Reserves tickets from inventory.
     *
     * @param quantity Number of tickets to reserve
     * @return Updated inventory with reserved tickets
     * @throws InsufficientInventoryException if not enough tickets available
     */
    public TicketInventory reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new InsufficientInventoryException(
                    "Not enough tickets available. Requested: " + quantity + 
                    ", Available: " + availableQuantity
            );
        }
        
        return this
                .withAvailableQuantity(availableQuantity - quantity)
                .withReservedQuantity(reservedQuantity + quantity)
                .withVersion(version + 1);
    }

    /**
     * Confirms reserved tickets (converts reservation to sold).
     *
     * @param quantity Number of tickets to confirm
     * @return Updated inventory
     */
    public TicketInventory confirmReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: " + reservedQuantity + 
                    ", Requested: " + quantity
            );
        }
        
        return this
                .withReservedQuantity(reservedQuantity - quantity)
                .withVersion(version + 1);
    }

    /**
     * Releases reserved tickets back to available inventory.
     *
     * @param quantity Number of tickets to release
     * @return Updated inventory
     */
    public TicketInventory releaseReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Not enough reserved tickets. Reserved: " + reservedQuantity + 
                    ", Requested: " + quantity
            );
        }
        
        return this
                .withReservedQuantity(reservedQuantity - quantity)
                .withAvailableQuantity(availableQuantity + quantity)
                .withVersion(version + 1);
    }

    /**
     * Checks if the requested quantity is available.
     *
     * @param quantity Quantity to check
     * @return true if available, false otherwise
     */
    public boolean isAvailable(int quantity) {
        return availableQuantity >= quantity;
    }
}
