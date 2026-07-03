package com.example.banking.services;

import org.springframework.stereotype.Service;

import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.models.StandingOrderLifecycleState;

@Service
public class StandingOrderLifecyclePolicyService {
    public void enforceUpdatableState(StandingOrderLifecycleState lifecycleState) {
        if (lifecycleState == StandingOrderLifecycleState.CANCELLED
                || lifecycleState == StandingOrderLifecycleState.COMPLETED) {
            throw StandingOrderErrors.conflict("Standing order is no longer mutable", "lifecycleState");
        }
    }

    public StandingOrderLifecycleState pause(StandingOrderLifecycleState lifecycleState) {
        if (lifecycleState == StandingOrderLifecycleState.ACTIVE) {
            return StandingOrderLifecycleState.PAUSED;
        }
        throw StandingOrderErrors.conflict("Only ACTIVE standing orders can be paused", "lifecycleState");
    }

    public StandingOrderLifecycleState resume(StandingOrderLifecycleState lifecycleState) {
        if (lifecycleState == StandingOrderLifecycleState.PAUSED) {
            return StandingOrderLifecycleState.ACTIVE;
        }
        throw StandingOrderErrors.conflict("Only PAUSED standing orders can be resumed", "lifecycleState");
    }

    public StandingOrderLifecycleState cancel(StandingOrderLifecycleState lifecycleState) {
        if (lifecycleState == StandingOrderLifecycleState.ACTIVE || lifecycleState == StandingOrderLifecycleState.PAUSED) {
            return StandingOrderLifecycleState.CANCELLED;
        }
        throw StandingOrderErrors.conflict("Standing order is already terminal", "lifecycleState");
    }
}
