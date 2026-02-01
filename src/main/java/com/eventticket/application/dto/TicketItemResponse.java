package com.eventticket.application.dto;

import com.eventticket.domain.model.TicketItem;
import com.eventticket.domain.model.TicketStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for ticket item response.
 * Pure Java Record - Java 25 features.
 */
public record TicketItemResponse(
        String ticketId,
        String ticketType,
        String seatNumber,
        BigDecimal price,
        String currency,
        TicketStatus status,
        Instant statusChangedAt,
        boolean countsAsRevenue,
        BigDecimal accountingValue
) {
    /**
     * Converts domain model to DTO.
     *
     * @param item Domain ticket item
     * @return TicketItemResponse DTO
     */
    public static TicketItemResponse fromDomain(TicketItem item) {
        return new TicketItemResponse(
                item.getTicketId().value(),
                item.getTicketType(),
                item.getSeatNumber(),
                item.getPrice().amount(),
                item.getPrice().getCurrencyCode(),
                item.getStatus(),
                item.getStatusChangedAt(),
                item.countsAsRevenue(),
                item.getAccountingValue().amount()
        );
    }
}
