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

import jakarta.validation.Valid;

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

    @PostMapping("/deposit")
    public ResponseEntity<PostingResponseSchema> postDeposit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
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
