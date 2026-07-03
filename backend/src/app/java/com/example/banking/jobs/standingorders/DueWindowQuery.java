package com.example.banking.jobs.standingorders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.example.banking.lib.config.StandingOrderModuleConfig;

@Component
public class DueWindowQuery {
    private final StandingOrderModuleConfig standingOrderModuleConfig;

    public DueWindowQuery(StandingOrderModuleConfig standingOrderModuleConfig) {
        this.standingOrderModuleConfig = standingOrderModuleConfig;
    }

    public Window resolve(Instant nowUtc) {
        Instant now = nowUtc == null ? Instant.now() : nowUtc;
        Instant windowEnd = now.truncatedTo(ChronoUnit.SECONDS);
        Instant windowStart = windowEnd.minusSeconds(Math.max(1, standingOrderModuleConfig.getSchedulerWindowMinutes()) * 60L);
        return new Window(windowStart, windowEnd);
    }

    public record Window(Instant windowStartUtc, Instant windowEndUtc) {
    }
}
