package com.example.banking.jobs.standingorders;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.lib.StandingOrderScheduleCursorJpaRepository;
import com.example.banking.models.StandingOrderCursorStatus;
import com.example.banking.models.StandingOrderScheduleCursorEntity;

@Service
public class ScheduleCursorService {
    private final StandingOrderScheduleCursorJpaRepository cursorRepository;

    public ScheduleCursorService(StandingOrderScheduleCursorJpaRepository cursorRepository) {
        this.cursorRepository = cursorRepository;
    }

    @Transactional
    public StandingOrderScheduleCursorEntity claim(String workerId, DueWindowQuery.Window window) {
        StandingOrderScheduleCursorEntity cursor = new StandingOrderScheduleCursorEntity();
        cursor.setCursorId(UUID.randomUUID().toString());
        cursor.setWorkerId(workerId);
        cursor.setWindowStartUtc(window.windowStartUtc());
        cursor.setWindowEndUtc(window.windowEndUtc());
        cursor.setClaimedAtUtc(Instant.now());
        cursor.setStatus(StandingOrderCursorStatus.CLAIMED);
        return cursorRepository.save(cursor);
    }

    @Transactional
    public void markCompleted(String cursorId) {
        cursorRepository.findById(cursorId).ifPresent(cursor -> {
            cursor.setStatus(StandingOrderCursorStatus.COMPLETED);
            cursor.setCompletedAtUtc(Instant.now());
            cursorRepository.save(cursor);
        });
    }

    @Transactional
    public void markAbandoned(String cursorId) {
        cursorRepository.findById(cursorId).ifPresent(cursor -> {
            cursor.setStatus(StandingOrderCursorStatus.ABANDONED);
            cursor.setCompletedAtUtc(Instant.now());
            cursorRepository.save(cursor);
        });
    }
}
