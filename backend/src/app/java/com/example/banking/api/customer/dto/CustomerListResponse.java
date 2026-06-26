package com.example.banking.api.customer.dto;

import java.util.List;

public record CustomerListResponse(
        List<CustomerResponse> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}
