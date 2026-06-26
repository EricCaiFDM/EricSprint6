package com.example.banking.lib.scheduling;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;

import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.models.StandingOrderCadence;

@Service
public class StandingOrderScheduleCalculator {
    public Instant calculateInitialNextExecutionAt(
            Instant effectiveFromUtc,
            Instant effectiveToUtc,
            StandingOrderCadence cadence,
            Instant nowUtc) {
        validateEffectiveWindow(effectiveFromUtc, effectiveToUtc);

        if (cadence == null) {
            throw StandingOrderErrors.validation("cadence is required", "cadence");
        }

        Instant cursor = effectiveFromUtc;
        Instant now = nowUtc == null ? Instant.now() : nowUtc;

        while (cursor.isBefore(now)) {
            cursor = nextOccurrence(cursor, cadence);
            if (effectiveToUtc != null && cursor.isAfter(effectiveToUtc)) {
                return null;
            }
        }

        return cursor;
    }

    public Instant calculateNextExecutionAt(
            Instant currentDueAtUtc,
            StandingOrderCadence cadence,
            Instant effectiveToUtc) {
        if (currentDueAtUtc == null) {
            return null;
        }
        Instant next = nextOccurrence(currentDueAtUtc, cadence);
        if (effectiveToUtc != null && next.isAfter(effectiveToUtc)) {
            return null;
        }
        return next;
    }

    public void validateEffectiveWindow(Instant effectiveFromUtc, Instant effectiveToUtc) {
        if (effectiveFromUtc == null) {
            throw StandingOrderErrors.validation("effectiveFromUtc is required", "effectiveFromUtc");
        }

        if (effectiveToUtc != null && effectiveFromUtc.isAfter(effectiveToUtc)) {
            throw StandingOrderErrors.validation(
                    "effectiveFromUtc must be less than or equal to effectiveToUtc",
                    "effectiveToUtc");
        }
    }

    private Instant nextOccurrence(Instant baseline, StandingOrderCadence cadence) {
        ZonedDateTime utc = baseline.atZone(ZoneOffset.UTC);
        return switch (cadence) {
            case DAILY -> utc.plusDays(1).toInstant();
            case WEEKLY -> utc.plusWeeks(1).toInstant();
            case MONTHLY -> utc.plusMonths(1).toInstant();
        };
    }
}
