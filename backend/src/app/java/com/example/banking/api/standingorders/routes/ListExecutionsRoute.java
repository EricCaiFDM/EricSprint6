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

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Standing Orders")
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

    @Operation(
            summary = "List standing order executions",
            description = "Returns paginated execution events and outcomes for a single standing order.")
    @ApiResponse(
            responseCode = "200",
            description = "Standing order execution page",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StandingOrderExecutionListResponseSchema.class),
                    examples = @ExampleObject(value = "{\"items\":[{\"executionEventId\":\"9f9f2e54-5a9d-4f2e-9a4a-198610b2a680\",\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"executionState\":\"SUCCEEDED\",\"executedAtUtc\":\"2026-07-01T00:00:02Z\",\"resultingTransactionId\":\"8b7d5c3e-6d43-4e20-9b57-72f6d4028517\"}],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1}")))
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
