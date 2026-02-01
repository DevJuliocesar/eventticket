package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.OrderId;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.EventId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing a ticket order.
 * Immutable entity following Domain-Driven Design principles.
 * Using Java 25 features - immutable class with factory methods and wither pattern.
 */
public final class TicketOrder {
    
    private final OrderId orderId;
    private final CustomerId customerId;
    private final String orderNumber;
    private final EventId eventId;
    private final String eventName;
    private final OrderStatus status;
    private final List<TicketItem> tickets;
    private final Money totalAmount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int version;

    private TicketOrder(
            OrderId orderId,
            CustomerId customerId,
            String orderNumber,
            EventId eventId,
            String eventName,
            OrderStatus status,
            List<TicketItem> tickets,
            Money totalAmount,
            Instant createdAt,
            Instant updatedAt,
            int version
    ) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customerId = Objects.requireNonNull(customerId);
        this.orderNumber = Objects.requireNonNull(orderNumber);
        this.eventId = Objects.requireNonNull(eventId);
        this.eventName = Objects.requireNonNull(eventName);
        this.status = Objects.requireNonNull(status);
        this.tickets = List.copyOf(tickets);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.version = version;
    }

    /**
     * Creates a new ticket order.
     */
    public static TicketOrder create(
            CustomerId customerId,
            EventId eventId,
            String eventName,
            List<TicketItem> tickets
    ) {
        Money totalAmount = calculateTotal(tickets);
        Instant now = Instant.now();
        
        return new TicketOrder(
                OrderId.generate(),
                customerId,
                generateOrderNumber(),
                eventId,
                eventName,
                OrderStatus.RESERVED,
                tickets,
                totalAmount,
                now,
                now,
                0
        );
    }

    /**
     * Confirms the order (moves to pending confirmation).
     */
    public TicketOrder confirm() {
        if (status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Only reserved orders can be confirmed");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.PENDING_CONFIRMATION, tickets, totalAmount, createdAt,
                Instant.now(), version + 1);
    }

    /**
     * Marks order as sold (payment completed).
     */
    public TicketOrder markAsSold() {
        if (status != OrderStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Only pending confirmation orders can be marked as sold");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.SOLD, tickets, totalAmount, createdAt,
                Instant.now(), version + 1);
    }

    /**
     * Cancels the order.
     */
    public TicketOrder cancel() {
        if (status == OrderStatus.SOLD || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel sold or already cancelled orders");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.CANCELLED, tickets, totalAmount, createdAt,
                Instant.now(), version + 1);
    }

    /**
     * Marks order as expired (reservation timeout).
     */
    public TicketOrder markAsExpired() {
        if (status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Only reserved orders can expire");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.EXPIRED, tickets, totalAmount, createdAt,
                Instant.now(), version + 1);
    }

    private static Money calculateTotal(List<TicketItem> tickets) {
        return tickets.stream()
                .map(TicketItem::getPrice)
                .reduce(Money::add)
                .orElse(Money.zero());
    }

    private static String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    // Getters
    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public String getOrderNumber() { return orderNumber; }
    public EventId getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public OrderStatus getStatus() { return status; }
    public List<TicketItem> getTickets() { return tickets; }
    public Money getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        return o instanceof TicketOrder other && Objects.equals(orderId, other.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "TicketOrder[orderId=%s, status=%s, eventId=%s]".formatted(orderId, status, eventId);
    }
}
