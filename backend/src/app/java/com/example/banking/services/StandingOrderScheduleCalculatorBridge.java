package com.example.banking.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.banking.lib.scheduling.StandingOrderScheduleCalculator;
import com.example.banking.models.StandingOrderEntity;

@Service
public class StandingOrderScheduleCalculatorBridge {
    private final StandingOrderScheduleCalculator scheduleCalculator;

    public StandingOrderScheduleCalculatorBridge(StandingOrderScheduleCalculator scheduleCalculator) {
        this.scheduleCalculator = scheduleCalculator;
    }

    public Instant nextExecution(StandingOrderEntity standingOrder, Instant currentDueAtUtc) {
        return scheduleCalculator.calculateNextExecutionAt(
                currentDueAtUtc,
                standingOrder.getCadence(),
                standingOrder.getEffectiveToUtc());
    }
}
