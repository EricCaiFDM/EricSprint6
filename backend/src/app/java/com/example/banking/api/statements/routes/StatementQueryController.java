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
