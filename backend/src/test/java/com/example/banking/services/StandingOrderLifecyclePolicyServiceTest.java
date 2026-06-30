package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.models.StandingOrderLifecycleState;

class StandingOrderLifecyclePolicyServiceTest {

    private final StandingOrderLifecyclePolicyService service = new StandingOrderLifecyclePolicyService();

    @Test
    void enforceUpdatableStateRejectsTerminalStates() {
        service.enforceUpdatableState(StandingOrderLifecycleState.ACTIVE);
        service.enforceUpdatableState(StandingOrderLifecycleState.PAUSED);

        ApiErrorException cancelled = captureEnforceUpdatableStateError(StandingOrderLifecycleState.CANCELLED);
        assertEquals("STANDING_ORDER_CONFLICT", cancelled.getCode());
        assertEquals("lifecycleState", cancelled.getField());

        ApiErrorException completed = captureEnforceUpdatableStateError(StandingOrderLifecycleState.COMPLETED);
        assertEquals("STANDING_ORDER_CONFLICT", completed.getCode());
    }

    @Test
    void pauseResumeAndCancelHaveExpectedTransitionsAndConflicts() {
        assertEquals(StandingOrderLifecycleState.PAUSED, service.pause(StandingOrderLifecycleState.ACTIVE));
        assertEquals(StandingOrderLifecycleState.ACTIVE, service.resume(StandingOrderLifecycleState.PAUSED));
        assertEquals(StandingOrderLifecycleState.CANCELLED, service.cancel(StandingOrderLifecycleState.ACTIVE));
        assertEquals(StandingOrderLifecycleState.CANCELLED, service.cancel(StandingOrderLifecycleState.PAUSED));

                ApiErrorException pauseConflict = capturePauseError(StandingOrderLifecycleState.PAUSED);
        assertEquals("STANDING_ORDER_CONFLICT", pauseConflict.getCode());

                ApiErrorException resumeConflict = captureResumeError(StandingOrderLifecycleState.ACTIVE);
        assertEquals("STANDING_ORDER_CONFLICT", resumeConflict.getCode());

                ApiErrorException cancelConflict = captureCancelError(StandingOrderLifecycleState.COMPLETED);
        assertEquals("STANDING_ORDER_CONFLICT", cancelConflict.getCode());
    }

        private ApiErrorException captureEnforceUpdatableStateError(StandingOrderLifecycleState lifecycleState) {
                ApiErrorException exception = null;
                try {
                        service.enforceUpdatableState(lifecycleState);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException capturePauseError(StandingOrderLifecycleState lifecycleState) {
                ApiErrorException exception = null;
                try {
                        service.pause(lifecycleState);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureResumeError(StandingOrderLifecycleState lifecycleState) {
                ApiErrorException exception = null;
                try {
                        service.resume(lifecycleState);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }

        private ApiErrorException captureCancelError(StandingOrderLifecycleState lifecycleState) {
                ApiErrorException exception = null;
                try {
                        service.cancel(lifecycleState);
                } catch (ApiErrorException captured) {
                        exception = captured;
                }
                return exception;
        }
}
