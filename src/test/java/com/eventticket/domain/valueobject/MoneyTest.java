package com.eventticket.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Test
    @DisplayName("Should create money with BigDecimal and currency code")
    void shouldCreateMoneyWithBigDecimalAndCurrencyCode() {
        // When
        Money money = Money.of(new BigDecimal("100.50"), "USD");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should create money with double and currency code")
    void shouldCreateMoneyWithDoubleAndCurrencyCode() {
        // When
        Money money = Money.of(100.50, "USD");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should normalize amount scale to 2 decimal places")
    void shouldNormalizeAmountScaleTo2DecimalPlaces() {
        // When
        Money money = Money.of(new BigDecimal("100.555"), "USD");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.56"));
        assertThat(money.getAmount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should create zero money")
    void shouldCreateZeroMoney() {
        // When
        Money zero = Money.zero();

        // Then
        assertThat(zero.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.getCurrencyCode()).isEqualTo("USD");
        assertThat(zero.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should create USD money using factory method")
    void shouldCreateUsdMoneyUsingFactoryMethod() {
        // When
        Money money = Money.usd(100.50);

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        // When & Then
        assertThatThrownBy(() -> Money.of((BigDecimal) null, "USD"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowExceptionWhenCurrencyIsNull() {
        // When & Then
        assertThatThrownBy(() -> new Money(new BigDecimal("100.00"), (Currency) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Currency cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        // When & Then
        assertThatThrownBy(() -> Money.of(new BigDecimal("-100.00"), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be negative");
    }

    @Test
    @DisplayName("Should add money with same currency")
    void shouldAddMoneyWithSameCurrency() {
        // Given
        Money money1 = Money.of(100.00, "USD");
        Money money2 = Money.of(50.00, "USD");

        // When
        Money result = money1.add(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should throw exception when adding money with different currencies")
    void shouldThrowExceptionWhenAddingMoneyWithDifferentCurrencies() {
        // Given
        Money usd = Money.of(100.00, "USD");
        Money eur = Money.of(50.00, "EUR");

        // When & Then
        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("Should subtract money with same currency")
    void shouldSubtractMoneyWithSameCurrency() {
        // Given
        Money money1 = Money.of(100.00, "USD");
        Money money2 = Money.of(30.00, "USD");

        // When
        Money result = money1.subtract(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("Should throw exception when subtraction results in negative")
    void shouldThrowExceptionWhenSubtractionResultsInNegative() {
        // Given
        Money money1 = Money.of(50.00, "USD");
        Money money2 = Money.of(100.00, "USD");

        // When & Then
        assertThatThrownBy(() -> money1.subtract(money2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Result cannot be negative");
    }

    @Test
    @DisplayName("Should multiply money by integer")
    void shouldMultiplyMoneyByInteger() {
        // Given
        Money money = Money.of(25.00, "USD");

        // When
        Money result = money.multiply(4);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should multiply money by BigDecimal")
    void shouldMultiplyMoneyByBigDecimal() {
        // Given
        Money money = Money.of(100.00, "USD");

        // When
        Money result = money.multiply(new BigDecimal("1.5"));

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should compare money correctly")
    void shouldCompareMoneyCorrectly() {
        // Given
        Money money1 = Money.of(100.00, "USD");
        Money money2 = Money.of(50.00, "USD");
        Money money3 = Money.of(150.00, "USD");

        // When & Then
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isLessThan(money1)).isTrue();
        assertThat(money3.isGreaterThan(money1)).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when comparing money with different currencies")
    void shouldThrowExceptionWhenComparingMoneyWithDifferentCurrencies() {
        // Given
        Money usd = Money.of(100.00, "USD");
        Money eur = Money.of(50.00, "EUR");

        // When & Then
        assertThatThrownBy(() -> usd.isGreaterThan(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("Should check if money is zero")
    void shouldCheckIfMoneyIsZero() {
        // Given
        Money zero = Money.zero();
        Money nonZero = Money.of(100.00, "USD");

        // When & Then
        assertThat(zero.isZero()).isTrue();
        assertThat(nonZero.isZero()).isFalse();
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        Money original = Money.of(100.00, "USD");

        // When
        Money result = original.add(Money.of(50.00, "USD"));

        // Then
        assertThat(original.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(original).isNotSameAs(result);
    }

    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
        // Given
        Money money = Money.of(100.50, "USD");

        // When
        String result = money.toString();

        // Then
        assertThat(result).contains("USD");
        assertThat(result).contains("100.50");
    }
}
