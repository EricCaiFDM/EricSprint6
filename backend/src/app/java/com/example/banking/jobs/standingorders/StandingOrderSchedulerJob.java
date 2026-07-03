package com.example.banking.jobs.standingorders;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.banking.services.StandingOrderExecutionOrchestrator;

@Component
public class StandingOrderSchedulerJob {
    private final DueWindowQuery dueWindowQuery;
    private final ScheduleCursorService scheduleCursorService;
    private final StandingOrderExecutionOrchestrator executionOrchestrator;

    public StandingOrderSchedulerJob(
            DueWindowQuery dueWindowQuery,
            ScheduleCursorService scheduleCursorService,
            StandingOrderExecutionOrchestrator executionOrchestrator) {
        this.dueWindowQuery = dueWindowQuery;
        this.scheduleCursorService = scheduleCursorService;
        this.executionOrchestrator = executionOrchestrator;
    }

    @Scheduled(fixedDelayString = "${standing-order.scheduler-fixed-delay-ms:60000}")
    public void run() {
        DueWindowQuery.Window window = dueWindowQuery.resolve(null);
        var cursor = scheduleCursorService.claim("scheduler-main", window);

        try {
            executionOrchestrator.processWindow(window.windowStartUtc(), window.windowEndUtc());
            scheduleCursorService.markCompleted(cursor.getCursorId());
        } catch (Exception exception) {
            scheduleCursorService.markAbandoned(cursor.getCursorId());
        }
    }
}
