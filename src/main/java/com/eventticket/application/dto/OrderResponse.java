package com.eventticket.application.dto;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketOrder;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for ticket order response.
 */
@Builder
public record OrderResponse(
        String orderId,
        String customerId,
        String orderNumber,
        String eventId,
        String eventName,
        OrderStatus status,
        List<TicketItemResponse> tickets,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Converts domain model to DTO.
     *
     * @param order Domain order
     * @return OrderResponse DTO
     */
    public static OrderResponse fromDomain(TicketOrder order) {
        List<TicketItemResponse> ticketItems = order.getTickets().stream()
                .map(TicketItemResponse::fromDomain)
                .toList();
        
        return OrderResponse.builder()
                .orderId(order.getOrderId().getValue())
                .customerId(order.getCustomerId().getValue())
                .orderNumber(order.getOrderNumber())
                .eventId(order.getEventId().getValue())
                .eventName(order.getEventName())
                .status(order.getStatus())
                .tickets(ticketItems)
                .totalAmount(order.getTotalAmount().getAmount())
                .currency(order.getTotalAmount().getCurrencyCode())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
