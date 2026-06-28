package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "standing_order_schedule_cursors")
public class StandingOrderScheduleCursorEntity {
    @Id
    @Column(name = "cursor_id", nullable = false, length = 36)
    private String cursorId;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "window_start_utc", nullable = false)
    private Instant windowStartUtc;

    @Column(name = "window_end_utc", nullable = false)
    private Instant windowEndUtc;

    @Column(name = "claimed_at_utc", nullable = false)
    private Instant claimedAtUtc;

    @Column(name = "completed_at_utc")
    private Instant completedAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StandingOrderCursorStatus status;

    @PrePersist
    void onCreate() {
        if (cursorId == null) {
            cursorId = UUID.randomUUID().toString();
        }
        if (claimedAtUtc == null) {
            claimedAtUtc = Instant.now();
        }
    }

    public String getCursorId() {
        return cursorId;
    }

    public void setCursorId(String cursorId) {
        this.cursorId = cursorId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Instant getWindowStartUtc() {
        return windowStartUtc;
    }

    public void setWindowStartUtc(Instant windowStartUtc) {
        this.windowStartUtc = windowStartUtc;
    }

    public Instant getWindowEndUtc() {
        return windowEndUtc;
    }

    public void setWindowEndUtc(Instant windowEndUtc) {
        this.windowEndUtc = windowEndUtc;
    }

    public Instant getClaimedAtUtc() {
        return claimedAtUtc;
    }

    public void setClaimedAtUtc(Instant claimedAtUtc) {
        this.claimedAtUtc = claimedAtUtc;
    }

    public Instant getCompletedAtUtc() {
        return completedAtUtc;
    }

    public void setCompletedAtUtc(Instant completedAtUtc) {
        this.completedAtUtc = completedAtUtc;
    }

    public StandingOrderCursorStatus getStatus() {
        return status;
    }

    public void setStatus(StandingOrderCursorStatus status) {
        this.status = status;
    }
}
