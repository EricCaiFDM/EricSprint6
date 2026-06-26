package com.example.banking.api.account;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.account.dto.AccountListResponse;
import com.example.banking.api.account.dto.AccountResponse;
import com.example.banking.api.account.dto.CreateAccountRequest;
import com.example.banking.api.account.dto.DeleteAccountResponse;
import com.example.banking.api.account.dto.UpdateAccountRequest;
import com.example.banking.services.AccountService;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountController {
    private final AccountService accountService;
    private final CustomerPrincipalResolver principalResolver;

    public AccountController(AccountService accountService, CustomerPrincipalResolver principalResolver) {
        this.accountService = accountService;
        this.principalResolver = principalResolver;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountResponse response = accountService.createAccount(request, principal.userId(), principal.role());
        return ResponseEntity.created(URI.create("/accounts/" + response.accountId())).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable String accountId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountResponse response = accountService.getAccountById(accountId, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<AccountListResponse> listAccounts(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountListResponse response = accountService.listAccounts(
                customerId,
                page,
                pageSize,
                accountType,
                status,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountResponse response = accountService.updateAccount(accountId, request, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(
            @PathVariable String accountId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        String status = accountService.deleteAccount(accountId, principal.userId(), principal.role());
        return ResponseEntity.ok(new DeleteAccountResponse(status, "Account removed from normal operational access"));
    }
}
