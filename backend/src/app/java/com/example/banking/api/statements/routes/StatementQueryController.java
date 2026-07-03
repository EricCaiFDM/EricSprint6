package com.example.banking.api.statements.routes;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.statements.StatementResponseMapper;
import com.example.banking.api.statements.schemas.StatementListResponseSchema;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.statement.StatementQueryService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Statements")
@RestController
@RequestMapping("/statements")
@Validated
public class StatementQueryController {
    private final StatementQueryService statementQueryService;
    private final CustomerPrincipalResolver principalResolver;
    private final StatementResponseMapper statementResponseMapper;

    public StatementQueryController(
            StatementQueryService statementQueryService,
            CustomerPrincipalResolver principalResolver,
            StatementResponseMapper statementResponseMapper) {
        this.statementQueryService = statementQueryService;
        this.principalResolver = principalResolver;
        this.statementResponseMapper = statementResponseMapper;
    }

    @Operation(
            summary = "List statements",
            description = "Returns paginated statements for an account, with optional filtering by statement month.")
    @ApiResponse(
            responseCode = "200",
            description = "Statement page",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StatementListResponseSchema.class),
                    examples = @ExampleObject(value = "{\"items\":[{\"statementId\":\"8d8a1415-b894-4cc3-ac6e-4f6c9836d5a2\",\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"periodYearMonth\":\"2026-07\",\"status\":\"READY\",\"artifactVersion\":1,\"generatedAtUtc\":\"2026-07-31T23:59:59Z\"}],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1}")))
    @GetMapping
    public ResponseEntity<StatementListResponseSchema> list(
            @RequestParam String accountId,
            @RequestParam(required = false) String periodYearMonth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);

        Page<MonthlyStatement> resultPage = statementQueryService.list(
                principal.userId(),
                principal.role(),
                accountId,
                periodYearMonth,
                page,
                pageSize);

        StatementListResponseSchema response = new StatementListResponseSchema(
                resultPage.getContent().stream().map(statementResponseMapper::toListItem).toList(),
                resultPage.getNumber() + 1,
                resultPage.getSize(),
                resultPage.getTotalElements(),
                Math.max(1, resultPage.getTotalPages()));

        return ResponseEntity.ok(response);
    }
}
