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
     * Orders are created in AVAILABLE status and will be reserved when processed.
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
                OrderStatus.AVAILABLE,
                tickets,
                totalAmount,
                now,
                now,
                0
        );
    }

    /**
     * Reserves the order (moves from AVAILABLE to RESERVED).
     * This happens when the order is processed asynchronously.
     */
    public TicketOrder reserve() {
        if (status != OrderStatus.AVAILABLE) {
            throw new IllegalStateException("Only available orders can be reserved");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.RESERVED, tickets, totalAmount, createdAt,
                Instant.now(), version + 1);
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
     * Updates tickets with assigned seat numbers.
     */
    public TicketOrder markAsSold(List<TicketItem> updatedTickets) {
        if (status != OrderStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Only pending confirmation orders can be marked as sold");
        }
        if (updatedTickets.isEmpty()) {
            throw new IllegalArgumentException("Updated tickets list cannot be empty");
        }
        // When tickets are stored separately (e.g., in DynamoDB), the tickets list may be empty
        // In that case, we only validate that we're providing at least one ticket
        if (!tickets.isEmpty() && updatedTickets.size() != tickets.size()) {
            throw new IllegalArgumentException("Updated tickets count must match original tickets count");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.SOLD, updatedTickets, totalAmount, createdAt,
                Instant.now(), version + 1);
    }

    /**
     * Marks order as complimentary (free tickets).
     * Business Rule: COMPLIMENTARY is final but not counted as revenue.
     * Can be applied from AVAILABLE, RESERVED, or PENDING_CONFIRMATION status.
     * Updates tickets with assigned seat numbers and sets total amount to zero.
     */
    public TicketOrder markAsComplimentary(List<TicketItem> updatedTickets) {
        if (status == OrderStatus.SOLD || status == OrderStatus.COMPLIMENTARY 
                || status == OrderStatus.EXPIRED || status == OrderStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot mark order as complimentary from %s status".formatted(status));
        }
        if (updatedTickets.isEmpty()) {
            throw new IllegalArgumentException("Updated tickets list cannot be empty");
        }
        return new TicketOrder(orderId, customerId, orderNumber, eventId, eventName,
                OrderStatus.COMPLIMENTARY, updatedTickets, Money.zero(), createdAt,
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
