package com.eventticket.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomerId Value Object Tests")
class CustomerIdTest {

    @Test
    @DisplayName("Should create CustomerId from string")
    void shouldCreateCustomerIdFromString() {
        // Given
        String value = "customer-123";

        // When
        CustomerId customerId = CustomerId.of(value);

        // Then
        assertThat(customerId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("Should throw exception when value is null")
    void shouldThrowExceptionWhenValueIsNull() {
        // When & Then
        assertThatThrownBy(() -> CustomerId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerId cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when value is blank")
    void shouldThrowExceptionWhenValueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> CustomerId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerId cannot be null or empty");
    }

    @Test
    @DisplayName("Should be equal when values are same")
    void shouldBeEqualWhenValuesAreSame() {
        // Given
        String value = "customer-123";
        CustomerId id1 = CustomerId.of(value);
        CustomerId id2 = CustomerId.of(value);

        // When & Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values are different")
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        CustomerId id1 = CustomerId.of("customer-123");
        CustomerId id2 = CustomerId.of("customer-456");

        // When & Then
        assertThat(id1).isNotEqualTo(id2);
    }
}
