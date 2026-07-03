package com.example.banking.api.standingorders.routes;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.standingorders.StandingOrderResponseMapper;
import com.example.banking.api.standingorders.schemas.CreateStandingOrderSchema;
import com.example.banking.api.standingorders.schemas.StandingOrderResponseSchema;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.services.CreateStandingOrderService;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Standing Orders")
@RestController
@RequestMapping("/standing-orders")
@Validated
public class CreateStandingOrderRoute {
    private final CreateStandingOrderService createStandingOrderService;
    private final CustomerPrincipalResolver principalResolver;
    private final StandingOrderResponseMapper responseMapper;

    public CreateStandingOrderRoute(
            CreateStandingOrderService createStandingOrderService,
            CustomerPrincipalResolver principalResolver,
            StandingOrderResponseMapper responseMapper) {
        this.createStandingOrderService = createStandingOrderService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Create standing order",
            description = "Creates a recurring payment instruction with cadence and execution policy within authorized scope.")
        @ApiResponse(
            responseCode = "201",
            description = "Standing order created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandingOrderResponseSchema.class),
                examples = @ExampleObject(value = "{\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"sourceAccountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"destinationAccountId\":\"a22f17f2-2122-4bb9-9e18-5f9dc9634df3\",\"amount\":\"120.00\",\"currencyCode\":\"AUD\",\"cadence\":\"MONTHLY\",\"lifecycleState\":\"ACTIVE\",\"nextExecutionAtUtc\":\"2026-08-01T00:00:00Z\"}")))
    @PostMapping
    public ResponseEntity<StandingOrderResponseSchema> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateStandingOrderSchema.class),
                    examples = @ExampleObject(value = "{\"sourceAccountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"destinationAccountId\":\"a22f17f2-2122-4bb9-9e18-5f9dc9634df3\",\"amount\":120.00,\"currencyCode\":\"AUD\",\"cadence\":\"MONTHLY\",\"startDate\":\"2026-08-01\"}")))
            @Valid @RequestBody CreateStandingOrderSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity created = createStandingOrderService.create(request, principal.userId(), principal.role());
        StandingOrderResponseSchema response = responseMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/standing-orders/" + response.standingOrderId())).body(response);
    }
}
