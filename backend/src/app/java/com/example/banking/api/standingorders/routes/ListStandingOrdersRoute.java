package com.example.banking.api.standingorders.routes;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.standingorders.StandingOrderResponseMapper;
import com.example.banking.api.standingorders.schemas.StandingOrderListResponseSchema;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.ListStandingOrdersService;

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
public class ListStandingOrdersRoute {
    private final ListStandingOrdersService listStandingOrdersService;
    private final CustomerPrincipalResolver principalResolver;
    private final StandingOrderResponseMapper responseMapper;

    public ListStandingOrdersRoute(
            ListStandingOrdersService listStandingOrdersService,
            CustomerPrincipalResolver principalResolver,
            StandingOrderResponseMapper responseMapper) {
        this.listStandingOrdersService = listStandingOrdersService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "List standing orders",
            description = "Returns paginated standing order instructions visible to the authenticated user scope.")
    @ApiResponse(
            responseCode = "200",
            description = "Standing orders page",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StandingOrderListResponseSchema.class),
                    examples = @ExampleObject(value = "{\"items\":[{\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"sourceAccountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"destinationAccountId\":\"a22f17f2-2122-4bb9-9e18-5f9dc9634df3\",\"amount\":\"120.00\",\"currencyCode\":\"AUD\",\"cadence\":\"MONTHLY\",\"lifecycleState\":\"ACTIVE\",\"nextExecutionAtUtc\":\"2026-08-01T00:00:00Z\"}],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1}")))
    @GetMapping
    public ResponseEntity<StandingOrderListResponseSchema> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);

        Page<StandingOrderEntity> resultPage = listStandingOrdersService.listByScope(
                principal.userId(),
                principal.role(),
                page,
                pageSize);

        StandingOrderListResponseSchema response = new StandingOrderListResponseSchema(
                resultPage.getContent().stream().map(responseMapper::toResponse).toList(),
                Math.max(1, page),
                Math.max(1, pageSize),
                resultPage.getTotalElements(),
                Math.max(1, resultPage.getTotalPages()));

        return ResponseEntity.ok(response);
    }
}
