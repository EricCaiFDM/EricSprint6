package com.example.banking.api.transactions.routes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.transactions.schemas.HistorySchema;
import com.example.banking.api.transactions.schemas.TransactionHistoryResponseSchema;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.TransactionHistoryService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Transactions")
@RestController
@RequestMapping("/transactions")
@Validated
public class HistoryRoute {
    private final TransactionHistoryService transactionHistoryService;
    private final CustomerPrincipalResolver principalResolver;

    public HistoryRoute(TransactionHistoryService transactionHistoryService, CustomerPrincipalResolver principalResolver) {
        this.transactionHistoryService = transactionHistoryService;
        this.principalResolver = principalResolver;
    }

    @Operation(
            summary = "Get transaction history",
            description = "Returns paginated transaction history filtered by scope, date range, and other optional query criteria.")
        @ApiResponse(
            responseCode = "200",
            description = "Transaction history page",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionHistoryResponseSchema.class),
                examples = @ExampleObject(value = "{\"items\":[{\"transactionId\":\"8b7d5c3e-6d43-4e20-9b57-72f6d4028517\",\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"transactionType\":\"DEPOSIT\",\"amount\":\"250.00\",\"currencyCode\":\"AUD\",\"description\":\"Cash deposit\",\"postedAtUtc\":\"2026-07-02T04:10:00Z\",\"balanceAfter\":\"1750.00\"}],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1}")))
    @GetMapping("/history")
    public ResponseEntity<TransactionHistoryResponseSchema> getHistory(
            @Valid @ModelAttribute HistorySchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        TransactionHistoryResponseSchema response = transactionHistoryService.getHistory(
                request,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(response);
    }
}
