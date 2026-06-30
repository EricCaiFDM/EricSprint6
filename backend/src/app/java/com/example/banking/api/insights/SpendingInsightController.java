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

import jakarta.validation.Valid;

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
