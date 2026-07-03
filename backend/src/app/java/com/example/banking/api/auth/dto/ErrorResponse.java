package com.example.banking.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for error response payload.")
public record ErrorResponse(String code, String message, String field) {
	public ErrorResponse(String code, String message) {
		this(code, message, null);
	}
}
