package com.example.banking.lib.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.errors.TransactionErrors;

@Service
public class MoneyPolicy {
    private static final int SCALE = 2;

    private final TransactionModuleConfig config;

    public MoneyPolicy(TransactionModuleConfig config) {
        this.config = config;
    }

    public BigDecimal parsePositiveAmount(String rawAmount, String field) {
        if (rawAmount == null || rawAmount.isBlank()) {
            throw TransactionErrors.validation(field + " is required", field);
        }
        try {
            return normalizePositiveAmount(new BigDecimal(rawAmount.trim()), field);
        } catch (NumberFormatException exception) {
            throw TransactionErrors.validation(field + " must be a decimal value", field);
        }
    }

    public BigDecimal normalizePositiveAmount(BigDecimal amount, String field) {
        if (amount == null) {
            throw TransactionErrors.validation(field + " is required", field);
        }
        BigDecimal scaled = amount.setScale(SCALE, RoundingMode.HALF_EVEN);
        if (scaled.signum() <= 0) {
            throw TransactionErrors.validation(field + " must be greater than zero", field);
        }
        return scaled;
    }

    public BigDecimal normalizeBalance(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_EVEN);
        }
        return amount.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal credit(BigDecimal currentBalance, BigDecimal amount) {
        BigDecimal normalizedCurrent = normalizeBalance(currentBalance);
        BigDecimal normalizedAmount = normalizePositiveAmount(amount, "amount");
        return normalizedCurrent.add(normalizedAmount).setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal debit(BigDecimal currentBalance, BigDecimal amount) {
        BigDecimal normalizedCurrent = normalizeBalance(currentBalance);
        BigDecimal normalizedAmount = normalizePositiveAmount(amount, "amount");
        if (normalizedCurrent.compareTo(normalizedAmount) < 0) {
            throw TransactionErrors.insufficientFunds();
        }
        return normalizedCurrent.subtract(normalizedAmount).setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public String normalizeCurrency(String currencyCode, String field) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw TransactionErrors.validation(field + " is required", field);
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw TransactionErrors.validation(field + " must be a 3-letter ISO code", field);
        }
        return normalized;
    }

    public void ensureSupportedCurrency(String currencyCode, String field) {
        String normalized = normalizeCurrency(currencyCode, field);
        String expected = config.getDefaultCurrencyCode();
        if (expected != null && !expected.isBlank() && !expected.equalsIgnoreCase(normalized)) {
            throw TransactionErrors.validation(
                    "Unsupported currency. Expected " + expected.toUpperCase(Locale.ROOT),
                    field);
        }
    }

    public void ensureSameCurrency(String leftCurrency, String rightCurrency, String field) {
        String left = normalizeCurrency(leftCurrency, field);
        String right = normalizeCurrency(rightCurrency, field);
        if (!left.equalsIgnoreCase(right)) {
            throw TransactionErrors.validation("Currency mismatch", field);
        }
    }
}
