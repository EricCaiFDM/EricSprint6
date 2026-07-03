package com.example.banking.api.insights;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.insights.SpendingInsightService;
import com.example.banking.services.insights.SpendingInsightServiceResult;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Insights")
@RestController
@RequestMapping("/insights")
@Validated
public class SpendingInsightController {
    private final SpendingInsightService spendingInsightService;
    private final SpendingInsightResponseMapper responseMapper;
    private final CustomerPrincipalResolver principalResolver;

    public SpendingInsightController(
            SpendingInsightService spendingInsightService,
            SpendingInsightResponseMapper responseMapper,
            CustomerPrincipalResolver principalResolver) {
        this.spendingInsightService = spendingInsightService;
        this.responseMapper = responseMapper;
        this.principalResolver = principalResolver;
    }

    @Operation(
            summary = "Get spending insights",
            description = "Returns categorized spending summaries, trends, and confidence metadata for the requested period scope.")
        @ApiResponse(
            responseCode = "200",
            description = "Spending insights",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SpendingInsightResponseSchema.class),
                examples = @ExampleObject(value = "{\"accountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"periodYearMonth\":\"2026-07\",\"totalDebit\":\"980.45\",\"totalCredit\":\"2100.00\",\"topCategories\":[{\"category\":\"Groceries\",\"amount\":\"320.10\"},{\"category\":\"Transport\",\"amount\":\"140.50\"}],\"trend\":\"DOWN\",\"generatedAtUtc\":\"2026-07-31T23:59:59Z\"}")))
    @GetMapping("/spending")
    public ResponseEntity<SpendingInsightResponseSchema> getSpendingInsights(
            @Valid @ModelAttribute SpendingInsightQuery query,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        SpendingInsightServiceResult result = spendingInsightService.getInsights(
                query,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(responseMapper.toSchema(result));
    }
}
