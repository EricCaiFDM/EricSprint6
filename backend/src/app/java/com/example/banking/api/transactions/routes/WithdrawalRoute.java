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

import jakarta.validation.Valid;

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

    @PostMapping("/withdrawal")
    public ResponseEntity<PostingResponseSchema> postWithdrawal(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
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
