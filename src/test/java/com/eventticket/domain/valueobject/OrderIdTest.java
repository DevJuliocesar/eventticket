package com.eventticket.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderId Value Object Tests")
class OrderIdTest {

    @Test
    @DisplayName("Should generate unique OrderId")
    void shouldGenerateUniqueOrderId() {
        // When
        OrderId id1 = OrderId.generate();
        OrderId id2 = OrderId.generate();

        // Then
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    @DisplayName("Should create OrderId from string")
    void shouldCreateOrderIdFromString() {
        // Given
        String value = "order-123";

        // When
        OrderId orderId = OrderId.of(value);

        // Then
        assertThat(orderId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("Should throw exception when value is null")
    void shouldThrowExceptionWhenValueIsNull() {
        // When & Then
        assertThatThrownBy(() -> OrderId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OrderId cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when value is blank")
    void shouldThrowExceptionWhenValueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> OrderId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OrderId cannot be null or empty");
    }

    @Test
    @DisplayName("Should be equal when values are same")
    void shouldBeEqualWhenValuesAreSame() {
        // Given
        String value = "order-123";
        OrderId id1 = OrderId.of(value);
        OrderId id2 = OrderId.of(value);

        // When & Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values are different")
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        OrderId id1 = OrderId.of("order-123");
        OrderId id2 = OrderId.of("order-456");

        // When & Then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
        // Given
        OrderId orderId = OrderId.of("order-123");

        // When
        String result = orderId.toString();

        // Then
        assertThat(result).contains("order-123");
    }
}
