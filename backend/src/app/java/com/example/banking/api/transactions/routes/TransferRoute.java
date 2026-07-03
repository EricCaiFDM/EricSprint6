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

import com.example.banking.api.transactions.schemas.TransferResponseSchema;
import com.example.banking.api.transactions.schemas.TransferSchema;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.TransferService;

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
public class TransferRoute {
    private final TransferService transferService;
    private final CustomerPrincipalResolver principalResolver;

    public TransferRoute(TransferService transferService, CustomerPrincipalResolver principalResolver) {
        this.transferService = transferService;
        this.principalResolver = principalResolver;
    }

    @Operation(
            summary = "Post transfer",
            description = "Moves funds between source and destination accounts with idempotency safeguards and audit tracking.")
        @ApiResponse(
            responseCode = "201",
            description = "Transfer posted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransferResponseSchema.class),
                examples = @ExampleObject(value = "{\"transferId\":\"f96f5bf6-c8e2-4f06-a40d-774f5969628f\",\"sourceTransactionId\":\"3f2f8302-27d7-4e23-b7c8-ec9078b8d341\",\"destinationTransactionId\":\"c1c0ef8e-a6b5-4d09-b2ab-902f7e1f8f74\",\"amount\":\"120.00\",\"currencyCode\":\"AUD\",\"postedAtUtc\":\"2026-07-02T04:20:00Z\"}")))
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseSchema> postTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TransferSchema.class),
                    examples = @ExampleObject(value = "{\"sourceAccountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"destinationAccountId\":\"a22f17f2-2122-4bb9-9e18-5f9dc9634df3\",\"amount\":120.00,\"currencyCode\":\"AUD\",\"description\":\"Rent split\"}")))
            @Valid @RequestBody TransferSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        TransferResponseSchema response = transferService.postTransfer(
                request,
                idempotencyKey,
                principal.userId(),
                principal.role());
        return ResponseEntity.created(URI.create("/transactions/" + response.transferId())).body(response);
    }
}
