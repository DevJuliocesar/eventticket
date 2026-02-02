package com.eventticket.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateEventRequest DTO Tests")
class CreateEventRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Should create valid request")
    void shouldCreateValidRequest() {
        // Given
        String name = "Concert";
        String description = "Music concert";
        String venue = "Stadium";
        Instant eventDate = Instant.now().plusSeconds(86400);
        Integer totalCapacity = 1000;

        // When
        CreateEventRequest request = new CreateEventRequest(name, description, venue, eventDate, totalCapacity);

        // Then
        assertThat(request.name()).isEqualTo(name);
        assertThat(request.description()).isEqualTo(description);
        assertThat(request.venue()).isEqualTo(venue);
        assertThat(request.eventDate()).isEqualTo(eventDate);
        assertThat(request.totalCapacity()).isEqualTo(totalCapacity);

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when name is blank")
    void shouldFailValidationWhenNameIsBlank() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "",
                "Description",
                "Venue",
                Instant.now().plusSeconds(86400),
                100
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Event name cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when description is blank")
    void shouldFailValidationWhenDescriptionIsBlank() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Name",
                "",
                "Venue",
                Instant.now().plusSeconds(86400),
                100
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Event description cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when venue is blank")
    void shouldFailValidationWhenVenueIsBlank() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Name",
                "Description",
                "",
                Instant.now().plusSeconds(86400),
                100
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Venue cannot be blank");
    }

    @Test
    @DisplayName("Should fail validation when eventDate is null")
    void shouldFailValidationWhenEventDateIsNull() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Name",
                "Description",
                "Venue",
                null,
                100
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Event date cannot be null");
    }

    @Test
    @DisplayName("Should fail validation when eventDate is in the past")
    void shouldFailValidationWhenEventDateIsInThePast() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Name",
                "Description",
                "Venue",
                Instant.now().minusSeconds(86400),
                100
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Event date must be in the future");
    }

    @Test
    @DisplayName("Should fail validation when totalCapacity is null")
    void shouldFailValidationWhenTotalCapacityIsNull() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Name",
                "Description",
                "Venue",
                Instant.now().plusSeconds(86400),
                null
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Total capacity cannot be null");
    }

    @Test
    @DisplayName("Should fail validation when totalCapacity is less than 1")
    void shouldFailValidationWhenTotalCapacityIsLessThan1() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Name",
                "Description",
                "Venue",
                Instant.now().plusSeconds(86400),
                0
        );

        // When
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Total capacity must be at least 1");
    }
}
