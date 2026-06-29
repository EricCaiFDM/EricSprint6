package com.example.banking.api.statements.routes;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.statements.schemas.GenerateStatementAcceptedResponseSchema;
import com.example.banking.api.statements.schemas.GenerateStatementRequestSchema;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.statement.StatementGenerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/statements")
@Validated
public class StatementGenerationController {
    private final StatementGenerationService statementGenerationService;
    private final CustomerPrincipalResolver principalResolver;

    public StatementGenerationController(
            StatementGenerationService statementGenerationService,
            CustomerPrincipalResolver principalResolver) {
        this.statementGenerationService = statementGenerationService;
        this.principalResolver = principalResolver;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateStatementAcceptedResponseSchema> generate(
            @Valid @RequestBody GenerateStatementRequestSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        MonthlyStatement statement = statementGenerationService.generate(
                request.accountId(),
                request.periodYearMonth(),
                request.generationMode(),
                principal.userId(),
                principal.role());

        GenerateStatementAcceptedResponseSchema response = new GenerateStatementAcceptedResponseSchema(
                statement.getStatementId(),
                "PROCESSING");

        return ResponseEntity.accepted()
                .location(URI.create("/statements/" + statement.getStatementId()))
                .body(response);
    }
}
