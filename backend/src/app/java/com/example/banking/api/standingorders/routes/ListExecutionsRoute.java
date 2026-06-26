package com.example.banking.api.standingorders.routes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.standingorders.StandingOrderResponseMapper;
import com.example.banking.api.standingorders.schemas.StandingOrderExecutionListResponseSchema;
import com.example.banking.models.StandingOrderExecutionEventEntity;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.ListStandingOrderExecutionsService;

@RestController
@RequestMapping("/standing-orders")
@Validated
public class ListExecutionsRoute {
    private final ListStandingOrderExecutionsService listExecutionsService;
    private final CustomerPrincipalResolver principalResolver;
    private final StandingOrderResponseMapper responseMapper;

    public ListExecutionsRoute(
            ListStandingOrderExecutionsService listExecutionsService,
            CustomerPrincipalResolver principalResolver,
            StandingOrderResponseMapper responseMapper) {
        this.listExecutionsService = listExecutionsService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/{standingOrderId}/executions")
    public ResponseEntity<StandingOrderExecutionListResponseSchema> listExecutions(
            @PathVariable String standingOrderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);

        org.springframework.data.domain.Page<StandingOrderExecutionEventEntity> resultPage = listExecutionsService.listExecutions(
                standingOrderId,
                page,
                pageSize,
                principal.userId(),
                principal.role());

        StandingOrderExecutionListResponseSchema response = new StandingOrderExecutionListResponseSchema(
                resultPage.getContent().stream().map(responseMapper::toExecutionItem).toList(),
                Math.max(1, page),
                Math.max(1, pageSize),
                resultPage.getTotalElements(),
                Math.max(1, resultPage.getTotalPages()));

        return ResponseEntity.ok(response);
    }
}
