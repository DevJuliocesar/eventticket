package com.eventticket.application.dto;

import com.eventticket.domain.model.OrderStatus;
import com.eventticket.domain.model.TicketOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for ticket order response.
 * Pure Java Record without Lombok - using Java 25 features.
 */
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
     * Converts domain model to DTO using Pattern Matching.
     *
     * @param order Domain order
     * @return OrderResponse DTO
     */
    public static OrderResponse fromDomain(TicketOrder order) {
        List<TicketItemResponse> ticketItems = order.getTickets().stream()
                .map(TicketItemResponse::fromDomain)
                .toList();
        
        return new OrderResponse(
                order.getOrderId().value(),
                order.getCustomerId().value(),
                order.getOrderNumber(),
                order.getEventId().value(),
                order.getEventName(),
                order.getStatus(),
                ticketItems,
                order.getTotalAmount().amount(),
                order.getTotalAmount().getCurrencyCode(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
