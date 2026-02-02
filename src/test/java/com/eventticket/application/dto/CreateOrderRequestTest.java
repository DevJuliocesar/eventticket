package com.eventticket.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateOrderRequest DTO Tests")
class CreateOrderRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Should create valid request")
    void shouldCreateValidRequest() {
        // Given
        String customerId = "customer-123";
        String eventId = "event-456";
        String eventName = "Concert";
        String ticketType = "VIP";
        Integer quantity = 2;

        // When
        CreateOrderRequest request = new CreateOrderRequest(customerId, eventId, eventName, ticketType, quantity);

        // Then
        assertThat(request.customerId()).isEqualTo(customerId);
        assertThat(request.eventId()).isEqualTo(eventId);
        assertThat(request.eventName()).isEqualTo(eventName);
        assertThat(request.ticketType()).isEqualTo(ticketType);
        assertThat(request.quantity()).isEqualTo(quantity);

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when customerId is blank")
    void shouldFailValidationWhenCustomerIdIsBlank() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("", "event-123", "Event", "VIP", 1);

        // When
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Customer ID cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when eventId is blank")
    void shouldFailValidationWhenEventIdIsBlank() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("customer-123", "", "Event", "VIP", 1);

        // When
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Event ID cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when eventName is blank")
    void shouldFailValidationWhenEventNameIsBlank() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("customer-123", "event-123", "", "VIP", 1);

        // When
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Event name cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when ticketType is blank")
    void shouldFailValidationWhenTicketTypeIsBlank() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("customer-123", "event-123", "Event", "", 1);

        // When
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Ticket type cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when quantity is null")
    void shouldFailValidationWhenQuantityIsNull() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("customer-123", "event-123", "Event", "VIP", null);

        // When
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Quantity cannot be null");
    }

    @Test
    @DisplayName("Should fail validation when quantity is less than 1")
    void shouldFailValidationWhenQuantityIsLessThan1() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("customer-123", "event-123", "Event", "VIP", 0);

        // When
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Quantity must be at least 1");
    }
}
