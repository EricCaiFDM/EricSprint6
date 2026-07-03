package com.example.banking.api.transactions.routes;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.transactions.schemas.PostingResponseSchema;
import com.example.banking.api.transactions.schemas.WithdrawalSchema;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.WithdrawalService;

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
public class WithdrawalRoute {
    private final WithdrawalService withdrawalService;
    private final CustomerPrincipalResolver principalResolver;

    public WithdrawalRoute(WithdrawalService withdrawalService, CustomerPrincipalResolver principalResolver) {
        this.withdrawalService = withdrawalService;
        this.principalResolver = principalResolver;
    }

    @Operation(
            summary = "Post withdrawal",
            description = "Debits funds from an account using idempotency protection and returns the posting transaction details.")
        @ApiResponse(
            responseCode = "201",
            description = "Withdrawal posted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PostingResponseSchema.class),
                examples = @ExampleObject(value = "{\"transactionId\":\"1b6d8f4c-2d4e-4f8f-8ccb-17cf7e9c916d\",\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"transactionType\":\"WITHDRAWAL\",\"amount\":\"80.00\",\"currencyCode\":\"AUD\",\"balanceAfter\":\"1670.00\",\"postedAtUtc\":\"2026-07-02T04:35:00Z\"}")))
    @PostMapping("/withdrawal")
    public ResponseEntity<PostingResponseSchema> postWithdrawal(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WithdrawalSchema.class),
                    examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"amount\":80.00,\"currencyCode\":\"AUD\",\"description\":\"ATM withdrawal\"}")))
            @Valid @RequestBody WithdrawalSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        PostingResponseSchema response = withdrawalService.postWithdrawal(
                request,
                idempotencyKey,
                principal.userId(),
                principal.role());
        return ResponseEntity.created(URI.create("/transactions/" + response.transactionId())).body(response);
    }
}
