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

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Statements")
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

    @Operation(
            summary = "Generate statement",
            description = "Initiates statement generation for an account and month and returns an accepted processing response for the requested period.")
    @ApiResponse(
            responseCode = "202",
            description = "Statement generation accepted",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GenerateStatementAcceptedResponseSchema.class),
                    examples = @ExampleObject(value = "{\"statementId\":\"8d8a1415-b894-4cc3-ac6e-4f6c9836d5a2\",\"status\":\"PROCESSING\"}")))
    @PostMapping("/generate")
    public ResponseEntity<GenerateStatementAcceptedResponseSchema> generate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenerateStatementRequestSchema.class),
                            examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"periodYearMonth\":\"2026-07\",\"generationMode\":\"STANDARD\"}")))
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
