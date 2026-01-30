package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.Money;
import com.eventticket.domain.valueobject.TicketId;
import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a ticket item in an order.
 */
@Value
@Builder
public class TicketItem {
    TicketId ticketId;
    String ticketType;
    String seatNumber;
    Money price;

    /**
     * Creates a new ticket item.
     *
     * @param ticketType Type of ticket (VIP, GENERAL, etc.)
     * @param seatNumber Seat number
     * @param price Ticket price
     * @return A new TicketItem instance
     */
    public static TicketItem create(String ticketType, String seatNumber, Money price) {
        return TicketItem.builder()
                .ticketId(TicketId.generate())
                .ticketType(ticketType)
                .seatNumber(seatNumber)
                .price(price)
                .build();
    }
}
