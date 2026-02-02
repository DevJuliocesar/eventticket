package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing customer information for an order.
 * Stores payment and contact information.
 * Using Java 25 - immutable class.
 */
public final class CustomerInfo {
    
    private final CustomerId customerId;
    private final OrderId orderId;
    private final String customerName;
    private final String email;
    private final String phoneNumber;
    private final String address;
    private final String city;
    private final String country;
    private final String paymentMethod;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CustomerInfo(
            CustomerId customerId,
            OrderId orderId,
            String customerName,
            String email,
            String phoneNumber,
            String address,
            String city,
            String country,
            String paymentMethod,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.customerId = Objects.requireNonNull(customerId);
        this.orderId = Objects.requireNonNull(orderId);
        this.customerName = Objects.requireNonNull(customerName);
        this.email = Objects.requireNonNull(email);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.address = address;
        this.city = city;
        this.country = country;
        this.paymentMethod = paymentMethod;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /**
     * Creates a new customer info record.
     */
    public static CustomerInfo create(
            CustomerId customerId,
            OrderId orderId,
            String customerName,
            String email,
            String phoneNumber,
            String address,
            String city,
            String country,
            String paymentMethod
    ) {
        Instant now = Instant.now();
        return new CustomerInfo(
                customerId,
                orderId,
                customerName,
                email,
                phoneNumber,
                address,
                city,
                country,
                paymentMethod,
                now,
                now
        );
    }

    /**
     * Updates customer information.
     */
    public CustomerInfo update(
            String customerName,
            String email,
            String phoneNumber,
            String address,
            String city,
            String country,
            String paymentMethod
    ) {
        return new CustomerInfo(
                this.customerId,
                this.orderId,
                customerName != null ? customerName : this.customerName,
                email != null ? email : this.email,
                phoneNumber != null ? phoneNumber : this.phoneNumber,
                address != null ? address : this.address,
                city != null ? city : this.city,
                country != null ? country : this.country,
                paymentMethod != null ? paymentMethod : this.paymentMethod,
                this.createdAt,
                Instant.now()
        );
    }

    // Getters
    public CustomerId getCustomerId() { return customerId; }
    public OrderId getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getPaymentMethod() { return paymentMethod; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof CustomerInfo other && Objects.equals(orderId, other.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "CustomerInfo[customerId=%s, orderId=%s, email=%s]"
                .formatted(customerId, orderId, email);
    }
}
