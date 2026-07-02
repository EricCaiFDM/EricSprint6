package com.example.banking.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for register response payload.")
public record RegisterResponse(String status, String userId) {
}
