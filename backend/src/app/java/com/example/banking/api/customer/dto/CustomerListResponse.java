package com.example.banking.api.customer.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for customer list response payload.")
public record CustomerListResponse(
        List<CustomerResponse> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}
