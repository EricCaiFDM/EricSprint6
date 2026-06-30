package com.example.banking.lib.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.config.TransactionModuleConfig;

class MoneyPolicyTest {

    private final MoneyPolicy moneyPolicy = new MoneyPolicy(config("USD"));

    @Test
    void parsePositiveAmountValidatesBlankAndDecimalFormat() {
        ApiErrorException required = captureParsePositiveAmountError(" ", "amount");
        assertEquals("TRANSACTION_VALIDATION_ERROR", required.getCode());
        assertEquals("amount", required.getField());

        ApiErrorException decimal = captureParsePositiveAmountError("abc", "amount");
        assertEquals("TRANSACTION_VALIDATION_ERROR", decimal.getCode());
        assertEquals("amount", decimal.getField());
    }

    @Test
    void normalizePositiveAmountRequiresPositiveValueAndRoundsHalfEven() {
        ApiErrorException required = captureNormalizePositiveAmountError(null, "amount");
        assertEquals("TRANSACTION_VALIDATION_ERROR", required.getCode());

        ApiErrorException positive = captureNormalizePositiveAmountError(new BigDecimal("0.00"), "amount");
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

        ApiErrorException insufficient = captureDebitError(new BigDecimal("1.00"), new BigDecimal("2.00"));
        assertEquals("TRANSACTION_INSUFFICIENT_FUNDS", insufficient.getCode());
    }

    @Test
    void currencyNormalizationAndSupportChecks() {
        assertEquals("USD", moneyPolicy.normalizeCurrency(" usd ", "currency"));

        ApiErrorException missing = captureNormalizeCurrencyError("", "currency");
        assertEquals("TRANSACTION_VALIDATION_ERROR", missing.getCode());

        ApiErrorException malformed = captureNormalizeCurrencyError("US", "currency");
        assertEquals("TRANSACTION_VALIDATION_ERROR", malformed.getCode());

        moneyPolicy.ensureSupportedCurrency("USD", "currency");

        ApiErrorException unsupported = captureEnsureSupportedCurrencyError("EUR", "currency");
        assertEquals("TRANSACTION_VALIDATION_ERROR", unsupported.getCode());

        moneyPolicy.ensureSameCurrency("usd", "USD", "currency");

                ApiErrorException mismatch = captureEnsureSameCurrencyError("USD", "EUR", "currency");
        assertEquals("TRANSACTION_VALIDATION_ERROR", mismatch.getCode());
    }

        private ApiErrorException captureParsePositiveAmountError(String rawAmount, String field) {
                ApiErrorException exception = null;
                try {
                        moneyPolicy.parsePositiveAmount(rawAmount, field);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureNormalizePositiveAmountError(BigDecimal amount, String field) {
                ApiErrorException exception = null;
                try {
                        moneyPolicy.normalizePositiveAmount(amount, field);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureDebitError(BigDecimal currentBalance, BigDecimal amount) {
                ApiErrorException exception = null;
                try {
                        moneyPolicy.debit(currentBalance, amount);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureNormalizeCurrencyError(String currency, String field) {
                ApiErrorException exception = null;
                try {
                        moneyPolicy.normalizeCurrency(currency, field);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureEnsureSupportedCurrencyError(String currency, String field) {
                ApiErrorException exception = null;
                try {
                        moneyPolicy.ensureSupportedCurrency(currency, field);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureEnsureSameCurrencyError(String sourceCurrency, String targetCurrency, String field) {
                ApiErrorException exception = null;
                try {
                        moneyPolicy.ensureSameCurrency(sourceCurrency, targetCurrency, field);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

    private TransactionModuleConfig config(String currency) {
        TransactionModuleConfig config = new TransactionModuleConfig();
        config.setDefaultCurrencyCode(currency);
        return config;
    }
}
