package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.models.StandingOrderLifecycleState;

class StandingOrderLifecyclePolicyServiceTest {

    private final StandingOrderLifecyclePolicyService service = new StandingOrderLifecyclePolicyService();

    @Test
    void enforceUpdatableStateRejectsTerminalStates() {
        service.enforceUpdatableState(StandingOrderLifecycleState.ACTIVE);
        service.enforceUpdatableState(StandingOrderLifecycleState.PAUSED);

        ApiErrorException cancelled = assertThrows(
                ApiErrorException.class,
                () -> service.enforceUpdatableState(StandingOrderLifecycleState.CANCELLED));
        assertEquals("STANDING_ORDER_CONFLICT", cancelled.getCode());
        assertEquals("lifecycleState", cancelled.getField());

        ApiErrorException completed = assertThrows(
                ApiErrorException.class,
                () -> service.enforceUpdatableState(StandingOrderLifecycleState.COMPLETED));
        assertEquals("STANDING_ORDER_CONFLICT", completed.getCode());
    }

    @Test
    void pauseResumeAndCancelHaveExpectedTransitionsAndConflicts() {
        assertEquals(StandingOrderLifecycleState.PAUSED, service.pause(StandingOrderLifecycleState.ACTIVE));
        assertEquals(StandingOrderLifecycleState.ACTIVE, service.resume(StandingOrderLifecycleState.PAUSED));
        assertEquals(StandingOrderLifecycleState.CANCELLED, service.cancel(StandingOrderLifecycleState.ACTIVE));
        assertEquals(StandingOrderLifecycleState.CANCELLED, service.cancel(StandingOrderLifecycleState.PAUSED));

        ApiErrorException pauseConflict = assertThrows(
                ApiErrorException.class,
                () -> service.pause(StandingOrderLifecycleState.PAUSED));
        assertEquals("STANDING_ORDER_CONFLICT", pauseConflict.getCode());

        ApiErrorException resumeConflict = assertThrows(
                ApiErrorException.class,
                () -> service.resume(StandingOrderLifecycleState.ACTIVE));
        assertEquals("STANDING_ORDER_CONFLICT", resumeConflict.getCode());

        ApiErrorException cancelConflict = assertThrows(
                ApiErrorException.class,
                () -> service.cancel(StandingOrderLifecycleState.COMPLETED));
        assertEquals("STANDING_ORDER_CONFLICT", cancelConflict.getCode());
    }
}
