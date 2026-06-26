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

import jakarta.validation.Valid;

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
