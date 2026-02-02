package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerInfo Domain Model Tests")
class CustomerInfoTest {

    @Test
    @DisplayName("Should create customer info with all fields")
    void shouldCreateCustomerInfoWithAllFields() {
        // Given
        CustomerId customerId = CustomerId.of("customer-123");
        OrderId orderId = OrderId.of("order-456");
        String customerName = "John Doe";
        String email = "john@example.com";
        String phoneNumber = "+1234567890";
        String address = "123 Main St";
        String city = "New York";
        String country = "USA";
        String paymentMethod = "Credit Card";

        // When
        CustomerInfo customerInfo = CustomerInfo.create(
                customerId, orderId, customerName, email, phoneNumber,
                address, city, country, paymentMethod
        );

        // Then
        assertThat(customerInfo.getCustomerId()).isEqualTo(customerId);
        assertThat(customerInfo.getOrderId()).isEqualTo(orderId);
        assertThat(customerInfo.getCustomerName()).isEqualTo(customerName);
        assertThat(customerInfo.getEmail()).isEqualTo(email);
        assertThat(customerInfo.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(customerInfo.getAddress()).isEqualTo(address);
        assertThat(customerInfo.getCity()).isEqualTo(city);
        assertThat(customerInfo.getCountry()).isEqualTo(country);
        assertThat(customerInfo.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(customerInfo.getCreatedAt()).isNotNull();
        assertThat(customerInfo.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update customer info with new values")
    void shouldUpdateCustomerInfoWithNewValues() {
        // Given
        CustomerInfo original = CustomerInfo.create(
                CustomerId.of("customer-123"),
                OrderId.of("order-456"),
                "John Doe",
                "john@example.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "USA",
                "Credit Card"
        );

        // When
        CustomerInfo updated = original.update(
                "Jane Doe",
                "jane@example.com",
                "+9876543210",
                "456 Oak Ave",
                "Los Angeles",
                "USA",
                "PayPal"
        );

        // Then
        assertThat(updated.getCustomerId()).isEqualTo(original.getCustomerId());
        assertThat(updated.getOrderId()).isEqualTo(original.getOrderId());
        assertThat(updated.getCustomerName()).isEqualTo("Jane Doe");
        assertThat(updated.getEmail()).isEqualTo("jane@example.com");
        assertThat(updated.getPhoneNumber()).isEqualTo("+9876543210");
        assertThat(updated.getAddress()).isEqualTo("456 Oak Ave");
        assertThat(updated.getCity()).isEqualTo("Los Angeles");
        assertThat(updated.getCountry()).isEqualTo("USA");
        assertThat(updated.getPaymentMethod()).isEqualTo("PayPal");
        assertThat(updated.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfter(original.getUpdatedAt());
    }

    @Test
    @DisplayName("Should preserve original values when updating with null")
    void shouldPreserveOriginalValuesWhenUpdatingWithNull() {
        // Given
        CustomerInfo original = CustomerInfo.create(
                CustomerId.of("customer-123"),
                OrderId.of("order-456"),
                "John Doe",
                "john@example.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "USA",
                "Credit Card"
        );

        // When
        CustomerInfo updated = original.update(null, null, null, null, null, null, null);

        // Then
        assertThat(updated.getCustomerName()).isEqualTo(original.getCustomerName());
        assertThat(updated.getEmail()).isEqualTo(original.getEmail());
        assertThat(updated.getPhoneNumber()).isEqualTo(original.getPhoneNumber());
        assertThat(updated.getAddress()).isEqualTo(original.getAddress());
        assertThat(updated.getCity()).isEqualTo(original.getCity());
        assertThat(updated.getCountry()).isEqualTo(original.getCountry());
        assertThat(updated.getPaymentMethod()).isEqualTo(original.getPaymentMethod());
    }

    @Test
    @DisplayName("Should be equal when orderIds are equal")
    void shouldBeEqualWhenOrderIdsAreEqual() {
        // Given
        OrderId orderId = OrderId.of("order-123");
        CustomerInfo info1 = CustomerInfo.create(
                CustomerId.of("customer-1"),
                orderId,
                "John Doe",
                "john@example.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "USA",
                "Credit Card"
        );
        CustomerInfo info2 = CustomerInfo.create(
                CustomerId.of("customer-2"),
                orderId,
                "Jane Doe",
                "jane@example.com",
                "+9876543210",
                "456 Oak Ave",
                "Los Angeles",
                "USA",
                "PayPal"
        );

        // Then
        assertThat(info1).isEqualTo(info2);
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when orderIds are different")
    void shouldNotBeEqualWhenOrderIdsAreDifferent() {
        // Given
        CustomerInfo info1 = CustomerInfo.create(
                CustomerId.of("customer-1"),
                OrderId.of("order-1"),
                "John Doe",
                "john@example.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "USA",
                "Credit Card"
        );
        CustomerInfo info2 = CustomerInfo.create(
                CustomerId.of("customer-1"),
                OrderId.of("order-2"),
                "John Doe",
                "john@example.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "USA",
                "Credit Card"
        );

        // Then
        assertThat(info1).isNotEqualTo(info2);
    }
}
