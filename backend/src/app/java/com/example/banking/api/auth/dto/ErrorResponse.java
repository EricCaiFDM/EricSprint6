package com.example.banking.api.auth.dto;

public record ErrorResponse(String code, String message, String field) {
	public ErrorResponse(String code, String message) {
		this(code, message, null);
	}
}
