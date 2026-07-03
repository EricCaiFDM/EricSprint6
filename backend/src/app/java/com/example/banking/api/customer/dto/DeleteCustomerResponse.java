package com.example.banking.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for delete customer response payload.")
public record DeleteCustomerResponse(String status, String message) {
}
