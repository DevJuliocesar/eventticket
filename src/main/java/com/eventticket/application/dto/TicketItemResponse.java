package com.eventticket.application.dto;

import com.eventticket.domain.model.TicketItem;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Data Transfer Object for ticket item response.
 */
@Builder
public record TicketItemResponse(
        String ticketId,
        String ticketType,
        String seatNumber,
        BigDecimal price,
        String currency
) {
    /**
     * Converts domain model to DTO.
     *
     * @param item Domain ticket item
     * @return TicketItemResponse DTO
     */
    public static TicketItemResponse fromDomain(TicketItem item) {
        return TicketItemResponse.builder()
                .ticketId(item.getTicketId().getValue())
                .ticketType(item.getTicketType())
                .seatNumber(item.getSeatNumber())
                .price(item.getPrice().getAmount())
                .currency(item.getPrice().getCurrencyCode())
                .build();
    }
}
