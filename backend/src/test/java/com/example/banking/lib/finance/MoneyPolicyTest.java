package com.example.banking.lib.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.config.TransactionModuleConfig;

class MoneyPolicyTest {

    private final MoneyPolicy moneyPolicy = new MoneyPolicy(config("USD"));

    @Test
    void parsePositiveAmountValidatesBlankAndDecimalFormat() {
        ApiErrorException required = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.parsePositiveAmount(" ", "amount"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", required.getCode());
        assertEquals("amount", required.getField());

        ApiErrorException decimal = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.parsePositiveAmount("abc", "amount"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", decimal.getCode());
        assertEquals("amount", decimal.getField());
    }

    @Test
    void normalizePositiveAmountRequiresPositiveValueAndRoundsHalfEven() {
        ApiErrorException required = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.normalizePositiveAmount(null, "amount"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", required.getCode());

        ApiErrorException positive = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.normalizePositiveAmount(new BigDecimal("0.00"), "amount"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", positive.getCode());

        BigDecimal rounded = moneyPolicy.normalizePositiveAmount(new BigDecimal("1.005"), "amount");
        assertEquals(new BigDecimal("1.00"), rounded);
    }

    @Test
    void normalizeBalanceCreditAndDebitBehaviors() {
        assertEquals(new BigDecimal("0.00"), moneyPolicy.normalizeBalance(null));
        assertEquals(new BigDecimal("12.34"), moneyPolicy.normalizeBalance(new BigDecimal("12.345")));

        assertEquals(
                new BigDecimal("15.33"),
                moneyPolicy.credit(new BigDecimal("10.11"), new BigDecimal("5.22")));

        assertEquals(
                new BigDecimal("4.89"),
                moneyPolicy.debit(new BigDecimal("10.11"), new BigDecimal("5.22")));

        ApiErrorException insufficient = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.debit(new BigDecimal("1.00"), new BigDecimal("2.00")));
        assertEquals("TRANSACTION_INSUFFICIENT_FUNDS", insufficient.getCode());
    }

    @Test
    void currencyNormalizationAndSupportChecks() {
        assertEquals("USD", moneyPolicy.normalizeCurrency(" usd ", "currency"));

        ApiErrorException missing = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.normalizeCurrency("", "currency"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", missing.getCode());

        ApiErrorException malformed = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.normalizeCurrency("US", "currency"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", malformed.getCode());

        moneyPolicy.ensureSupportedCurrency("USD", "currency");

        ApiErrorException unsupported = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.ensureSupportedCurrency("EUR", "currency"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", unsupported.getCode());

        moneyPolicy.ensureSameCurrency("usd", "USD", "currency");

        ApiErrorException mismatch = assertThrows(
                ApiErrorException.class,
                () -> moneyPolicy.ensureSameCurrency("USD", "EUR", "currency"));
        assertEquals("TRANSACTION_VALIDATION_ERROR", mismatch.getCode());
    }

    private TransactionModuleConfig config(String currency) {
        TransactionModuleConfig config = new TransactionModuleConfig();
        config.setDefaultCurrencyCode(currency);
        return config;
    }
}
