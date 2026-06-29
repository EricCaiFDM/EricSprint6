package com.example.banking.api.statements.routes;

import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.statements.StatementResponseMapper;
import com.example.banking.api.statements.schemas.StatementResponseSchema;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.statement.StatementAuthorizationService;

@RestController
@RequestMapping("/statements")
@Validated
public class StatementRetrievalController {
    private final StatementAuthorizationService statementAuthorizationService;
    private final CustomerPrincipalResolver principalResolver;
    private final StatementResponseMapper statementResponseMapper;

    public StatementRetrievalController(
            StatementAuthorizationService statementAuthorizationService,
            CustomerPrincipalResolver principalResolver,
            StatementResponseMapper statementResponseMapper) {
        this.statementAuthorizationService = statementAuthorizationService;
        this.principalResolver = principalResolver;
        this.statementResponseMapper = statementResponseMapper;
    }

    @GetMapping("/{statementId}")
    public ResponseEntity<StatementResponseSchema> getById(
            @PathVariable
            @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "statementId must be a UUID")
            String statementId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        MonthlyStatement statement = statementAuthorizationService.readStatementById(
                statementId,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(statementResponseMapper.toResponse(statement));
    }
}
