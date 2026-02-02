package com.eventticket.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfirmOrderRequest DTO Tests")
class ConfirmOrderRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Should create valid request")
    void shouldCreateValidRequest() {
        // Given
        String customerName = "John Doe";
        String email = "john@example.com";
        String phoneNumber = "+1234567890";
        String address = "123 Main St";
        String city = "New York";
        String country = "USA";
        String paymentMethod = "Credit Card";

        // When
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                customerName, email, phoneNumber, address, city, country, paymentMethod
        );

        // Then
        assertThat(request.customerName()).isEqualTo(customerName);
        assertThat(request.email()).isEqualTo(email);
        assertThat(request.phoneNumber()).isEqualTo(phoneNumber);
        assertThat(request.address()).isEqualTo(address);
        assertThat(request.city()).isEqualTo(city);
        assertThat(request.country()).isEqualTo(country);
        assertThat(request.paymentMethod()).isEqualTo(paymentMethod);

        Set<ConstraintViolation<ConfirmOrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when customerName is blank")
    void shouldFailValidationWhenCustomerNameIsBlank() {
        // Given
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                "", "john@example.com", "+1234567890", null, null, null, null
        );

        // When
        Set<ConstraintViolation<ConfirmOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Customer name cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when email is blank")
    void shouldFailValidationWhenEmailIsBlank() {
        // Given
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                "John Doe", "", "+1234567890", null, null, null, null
        );

        // When
        Set<ConstraintViolation<ConfirmOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Email cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when email is invalid")
    void shouldFailValidationWhenEmailIsInvalid() {
        // Given
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                "John Doe", "invalid-email", "+1234567890", null, null, null, null
        );

        // When
        Set<ConstraintViolation<ConfirmOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Email must be valid");
    }

    @Test
    @DisplayName("Should fail validation when phoneNumber is blank")
    void shouldFailValidationWhenPhoneNumberIsBlank() {
        // Given
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                "John Doe", "john@example.com", "", null, null, null, null
        );

        // When
        Set<ConstraintViolation<ConfirmOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Phone number cannot be blank");
    }

    @Test
    @DisplayName("Should accept null optional fields")
    void shouldAcceptNullOptionalFields() {
        // Given
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                "John Doe", "john@example.com", "+1234567890", null, null, null, null
        );

        // When
        Set<ConstraintViolation<ConfirmOrderRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }
}
