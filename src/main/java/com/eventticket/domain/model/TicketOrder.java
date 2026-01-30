package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * Domain entity representing a ticket order.
 * Immutable entity following Domain-Driven Design principles.
 */
@Value
@Builder
@With
public class TicketOrder {
    
    OrderId orderId;
    CustomerId customerId;
    String orderNumber;
    EventId eventId;
    String eventName;
    OrderStatus status;
    List<TicketItem> tickets;
    Money totalAmount;
    Instant createdAt;
    Instant updatedAt;
    int version;

    /**
     * Creates a new ticket order.
     *
     * @param customerId Customer identifier
     * @param eventId Event identifier
     * @param eventName Event name
     * @param tickets List of ticket items
     * @return A new TicketOrder instance
     */
    public static TicketOrder create(
            CustomerId customerId,
            EventId eventId,
            String eventName,
            List<TicketItem> tickets
    ) {
        Money totalAmount = calculateTotal(tickets);
        Instant now = Instant.now();
        
        return TicketOrder.builder()
                .orderId(OrderId.generate())
                .customerId(customerId)
                .orderNumber(generateOrderNumber())
                .eventId(eventId)
                .eventName(eventName)
                .status(OrderStatus.RESERVED)  // Start as RESERVED for 10 min timeout
                .tickets(List.copyOf(tickets))
                .totalAmount(totalAmount)
                .createdAt(now)
                .updatedAt(now)
                .version(0)
                .build();
    }

    /**
     * Confirms the order (moves to pending confirmation).
     *
     * @return A new TicketOrder with pending confirmation status
     */
    public TicketOrder confirm() {
        if (status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Only reserved orders can be confirmed");
        }
        return this
                .withStatus(OrderStatus.PENDING_CONFIRMATION)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Marks order as sold (payment completed).
     *
     * @return A new TicketOrder with sold status
     */
    public TicketOrder markAsSold() {
        if (status != OrderStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Only pending confirmation orders can be marked as sold");
        }
        return this
                .withStatus(OrderStatus.SOLD)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Cancels the order.
     *
     * @return A new TicketOrder with cancelled status
     */
    public TicketOrder cancel() {
        if (status == OrderStatus.SOLD || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel sold or already cancelled orders");
        }
        return this
                .withStatus(OrderStatus.CANCELLED)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    /**
     * Marks order as expired (reservation timeout).
     *
     * @return A new TicketOrder with expired status
     */
    public TicketOrder markAsExpired() {
        if (status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Only reserved orders can expire");
        }
        return this
                .withStatus(OrderStatus.EXPIRED)
                .withUpdatedAt(Instant.now())
                .withVersion(version + 1);
    }

    private static Money calculateTotal(List<TicketItem> tickets) {
        return tickets.stream()
                .map(TicketItem::price)
                .reduce(Money::add)
                .orElse(Money.zero());
    }

    private static String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}
