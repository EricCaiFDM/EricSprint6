package com.example.banking.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for generic acknowledge response payload.")
public record GenericAcknowledgeResponse(String status, String message) {
}
