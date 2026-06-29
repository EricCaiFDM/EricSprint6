package com.example.banking.models.statement;

public record StatementAccessPolicy(
        String userId,
        String role,
        String customerScopeId) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(role);
    }
}
