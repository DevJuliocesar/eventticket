package com.eventticket.application.dto;

import com.eventticket.domain.model.TicketInventory;

import java.math.BigDecimal;

/**
 * Data Transfer Object for ticket inventory response.
 */
public record InventoryResponse(
        String eventId,
        String ticketType,
        String eventName,
        Integer totalQuantity,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        BigDecimal priceAmount,
        String priceCurrency,
        Integer version
) {
    public static InventoryResponse fromDomain(TicketInventory inventory) {
        return new InventoryResponse(
                inventory.getEventId().value(),
                inventory.getTicketType(),
                inventory.getEventName(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getSoldQuantity(),
                inventory.getPrice().getAmount(),
                inventory.getPrice().getCurrencyCode(),
                inventory.getVersion()
        );
    }
}
