package com.example.banking.api.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for delete account response payload.")
public record DeleteAccountResponse(String status, String message) {
}
