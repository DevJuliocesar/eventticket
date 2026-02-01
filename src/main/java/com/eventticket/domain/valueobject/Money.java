package com.eventticket.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing monetary amount.
 * Immutable record with rich behavior for currency operations.
 * Using Java 25 Record with compact canonical constructor.
 */
public record Money(BigDecimal amount, Currency currency) {

    /**
     * Compact canonical constructor with validation.
     */
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        // Normalize amount scale
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    // Factory methods
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(double amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    }

    public static Money usd(double amount) {
        return of(amount, "USD");
    }

    // Arithmetic operations
    public Money add(Money other) {
        validateSameCurrency(other, "add");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other, "subtract");
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new Money(result, this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    // Comparison operations
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other, "compare");
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other, "compare");
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // Accessor methods (Java 25 style - using record accessor pattern)
    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    // Validation helper
    private void validateSameCurrency(Money other, String operation) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot %s money with different currencies: %s vs %s"
                    .formatted(operation, this.currency, other.currency)
            );
        }
    }

    @Override
    public String toString() {
        return "%s %s".formatted(currency.getCurrencyCode(), amount);
    }
}
