package com.example.banking.services;

class PasswordResetRequestService {
    private final AuthRepository repository;
    private final AuthAuditService audit;

    PasswordResetRequestService(AuthRepository repository, AuthAuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    GenericAck requestReset(String identity) {
        repository.findByEmail(identity).ifPresent(user ->
                audit.record("RESET_REQUEST", user.id(), "SUCCESS", null));
        if (repository.findByEmail(identity).isEmpty()) {
            audit.record("RESET_REQUEST", null, "SUCCESS", "NON_EXISTING_IDENTITY");
        }
        return new GenericAck("ACCEPTED", "If the account exists, reset instructions will be sent.");
    }

    record GenericAck(String status, String message) {}
}
