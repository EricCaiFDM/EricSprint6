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

import jakarta.validation.Valid;

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

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseSchema> postTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
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
