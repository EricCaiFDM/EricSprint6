package com.example.banking.api.auth.schemas;

class PasswordResetRequestSchema {
    String identity;
}

class PasswordResetAcknowledge {
    String status = "ACCEPTED";
    String message = "If the account exists, reset instructions will be sent.";
}
