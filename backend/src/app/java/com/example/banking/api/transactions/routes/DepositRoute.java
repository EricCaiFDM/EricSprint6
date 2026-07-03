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

import com.example.banking.api.transactions.schemas.DepositSchema;
import com.example.banking.api.transactions.schemas.PostingResponseSchema;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.DepositService;

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
public class DepositRoute {
    private final DepositService depositService;
    private final CustomerPrincipalResolver principalResolver;

    public DepositRoute(DepositService depositService, CustomerPrincipalResolver principalResolver) {
        this.depositService = depositService;
        this.principalResolver = principalResolver;
    }

    @Operation(
            summary = "Post deposit",
            description = "Credits funds to an account using idempotency protection and returns the posting transaction details.")
        @ApiResponse(
            responseCode = "201",
            description = "Deposit posted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PostingResponseSchema.class),
                examples = @ExampleObject(value = "{\"transactionId\":\"8b7d5c3e-6d43-4e20-9b57-72f6d4028517\",\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"transactionType\":\"DEPOSIT\",\"amount\":\"250.00\",\"currencyCode\":\"AUD\",\"balanceAfter\":\"1750.00\",\"postedAtUtc\":\"2026-07-02T04:10:00Z\"}")))
    @PostMapping("/deposit")
    public ResponseEntity<PostingResponseSchema> postDeposit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DepositSchema.class),
                    examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"amount\":250.00,\"currencyCode\":\"AUD\",\"description\":\"Cash deposit\"}")))
            @Valid @RequestBody DepositSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        PostingResponseSchema response = depositService.postDeposit(
                request,
                idempotencyKey,
                principal.userId(),
                principal.role());
        return ResponseEntity.created(URI.create("/transactions/" + response.transactionId())).body(response);
    }
}
