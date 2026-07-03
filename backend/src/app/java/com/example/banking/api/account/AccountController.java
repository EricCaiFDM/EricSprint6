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

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Accounts")
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

    @Operation(
            summary = "Create account",
            description = "Creates a new bank account within the authorized customer scope and returns account details.")
        @ApiResponse(
            responseCode = "201",
            description = "Account created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountResponse.class),
                examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"accountNumber\":\"1002003001\",\"checkingNumber\":1002003001,\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"accountType\":\"CHECKING\",\"interestRate\":\"0.50\",\"status\":\"ACTIVE\",\"currencyCode\":\"AUD\",\"nickname\":\"Daily spending\",\"balance\":\"1500.00\",\"availableBalance\":\"1500.00\",\"currentBalance\":\"1500.00\",\"openedAtUtc\":\"2026-07-01T10:00:00Z\",\"closedAtUtc\":\"\"}")))
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateAccountRequest.class),
                    examples = @ExampleObject(value = "{\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"accountType\":\"CHECKING\",\"currencyCode\":\"AUD\",\"nickname\":\"Daily spending\",\"interestRate\":0.50}")))
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountResponse response = accountService.createAccount(request, principal.userId(), principal.role());
        return ResponseEntity.created(URI.create("/accounts/" + response.accountId())).body(response);
    }

        @Operation(
            summary = "Get account by id",
            description = "Returns account details, balances, and metadata for a single authorized account identifier.")
        @ApiResponse(
            responseCode = "200",
            description = "Account details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountResponse.class),
                examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"accountNumber\":\"1002003001\",\"checkingNumber\":1002003001,\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"accountType\":\"CHECKING\",\"interestRate\":\"0.50\",\"status\":\"ACTIVE\",\"currencyCode\":\"AUD\",\"nickname\":\"Daily spending\",\"balance\":\"1500.00\",\"availableBalance\":\"1500.00\",\"currentBalance\":\"1500.00\",\"openedAtUtc\":\"2026-07-01T10:00:00Z\",\"closedAtUtc\":\"\"}")))
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable String accountId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountResponse response = accountService.getAccountById(accountId, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

        @Operation(
            summary = "List accounts",
            description = "Returns a paginated, filterable list of accounts for the selected customer scope.")
        @ApiResponse(
            responseCode = "200",
            description = "Account page",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountListResponse.class),
                examples = @ExampleObject(value = "{\"items\":[{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"accountNumber\":\"1002003001\",\"checkingNumber\":1002003001,\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"accountType\":\"CHECKING\",\"interestRate\":\"0.50\",\"status\":\"ACTIVE\",\"currencyCode\":\"AUD\",\"nickname\":\"Daily spending\",\"balance\":\"1500.00\",\"availableBalance\":\"1500.00\",\"currentBalance\":\"1500.00\",\"openedAtUtc\":\"2026-07-01T10:00:00Z\",\"closedAtUtc\":\"\"}],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1}")))
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

        @Operation(
            summary = "Update account",
            description = "Updates mutable account attributes such as nickname or lifecycle state for an authorized account.")
        @ApiResponse(
            responseCode = "200",
            description = "Account updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountResponse.class),
                examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"accountNumber\":\"1002003001\",\"checkingNumber\":1002003001,\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"accountType\":\"CHECKING\",\"interestRate\":\"0.75\",\"status\":\"ACTIVE\",\"currencyCode\":\"AUD\",\"nickname\":\"Bills\",\"balance\":\"1525.40\",\"availableBalance\":\"1525.40\",\"currentBalance\":\"1525.40\",\"openedAtUtc\":\"2026-07-01T10:00:00Z\",\"closedAtUtc\":\"\"}")))
    @PatchMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String accountId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateAccountRequest.class),
                    examples = @ExampleObject(value = "{\"nickname\":\"Bills\",\"status\":\"ACTIVE\",\"interestRate\":0.75,\"balance\":1525.40}")))
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        AccountResponse response = accountService.updateAccount(accountId, request, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

        @Operation(
            summary = "Delete account",
            description = "Removes an account from normal operational access according to account policy and authorization rules.")
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Account removed",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeleteAccountResponse.class),
                    examples = @ExampleObject(value = "{\"status\":\"DELETED\",\"message\":\"Account removed from normal operational access\"}")))
        })
    @DeleteMapping("/{accountId}")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(
            @PathVariable String accountId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        String status = accountService.deleteAccount(accountId, principal.userId(), principal.role());
        return ResponseEntity.ok(new DeleteAccountResponse(status, "Account removed from normal operational access"));
    }
}
