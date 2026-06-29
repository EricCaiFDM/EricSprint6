package com.example.banking.lib.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.models.StandingOrderCadence;

class StandingOrderScheduleCalculatorTest {

    private final StandingOrderScheduleCalculator calculator = new StandingOrderScheduleCalculator();

    @Test
    void calculateInitialNextExecutionAtReturnsEffectiveFromWhenAlreadyDueInFuture() {
        Instant now = Instant.parse("2026-06-29T10:00:00Z");
        Instant effectiveFrom = Instant.parse("2026-06-30T10:00:00Z");

        Instant next = calculator.calculateInitialNextExecutionAt(
                effectiveFrom,
                null,
                StandingOrderCadence.DAILY,
                now);

        assertEquals(effectiveFrom, next);
    }

    @Test
    void calculateInitialNextExecutionAtMovesForwardByCadenceUntilAfterNow() {
        Instant now = Instant.parse("2026-06-29T10:00:00Z");
        Instant effectiveFrom = Instant.parse("2026-06-26T10:00:00Z");

        Instant dailyNext = calculator.calculateInitialNextExecutionAt(
                effectiveFrom,
                null,
                StandingOrderCadence.DAILY,
                now);
        assertEquals(Instant.parse("2026-06-29T10:00:00Z"), dailyNext);

        Instant weeklyNext = calculator.calculateInitialNextExecutionAt(
                effectiveFrom,
                null,
                StandingOrderCadence.WEEKLY,
                now);
        assertEquals(Instant.parse("2026-07-03T10:00:00Z"), weeklyNext);

        Instant monthlyNext = calculator.calculateInitialNextExecutionAt(
                effectiveFrom,
                null,
                StandingOrderCadence.MONTHLY,
                now);
        assertEquals(Instant.parse("2026-07-26T10:00:00Z"), monthlyNext);
    }

    @Test
    void calculateInitialNextExecutionAtReturnsNullWhenEffectiveWindowEndsBeforeNextRun() {
        Instant now = Instant.parse("2026-06-29T10:00:00Z");
        Instant effectiveFrom = Instant.parse("2026-06-27T10:00:00Z");
        Instant effectiveTo = Instant.parse("2026-06-28T10:00:00Z");

        Instant next = calculator.calculateInitialNextExecutionAt(
                effectiveFrom,
                effectiveTo,
                StandingOrderCadence.DAILY,
                now);

        assertNull(next);
    }

    @Test
    void calculateInitialNextExecutionAtValidatesCadenceAndEffectiveWindow() {
        Instant now = Instant.parse("2026-06-29T10:00:00Z");
        Instant effectiveFrom = Instant.parse("2026-06-29T09:00:00Z");

        ApiErrorException cadenceError = assertThrows(
                ApiErrorException.class,
                () -> calculator.calculateInitialNextExecutionAt(effectiveFrom, null, null, now));
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", cadenceError.getCode());

        ApiErrorException missingFrom = assertThrows(
                ApiErrorException.class,
                () -> calculator.validateEffectiveWindow(null, null));
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", missingFrom.getCode());

        ApiErrorException invalidRange = assertThrows(
                ApiErrorException.class,
                () -> calculator.validateEffectiveWindow(
                        Instant.parse("2026-06-30T10:00:00Z"),
                        Instant.parse("2026-06-29T10:00:00Z")));
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", invalidRange.getCode());
    }

    @Test
    void calculateNextExecutionAtSupportsNullAndEffectiveEndBoundary() {
        assertNull(calculator.calculateNextExecutionAt(null, StandingOrderCadence.DAILY, null));

        Instant next = calculator.calculateNextExecutionAt(
                Instant.parse("2026-06-29T10:00:00Z"),
                StandingOrderCadence.DAILY,
                null);
        assertEquals(Instant.parse("2026-06-30T10:00:00Z"), next);

        Instant blockedByEnd = calculator.calculateNextExecutionAt(
                Instant.parse("2026-06-29T10:00:00Z"),
                StandingOrderCadence.DAILY,
                Instant.parse("2026-06-29T20:00:00Z"));
        assertNull(blockedByEnd);
    }
}
