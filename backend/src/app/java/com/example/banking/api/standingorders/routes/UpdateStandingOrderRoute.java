package com.example.banking.api.standingorders.routes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.standingorders.StandingOrderResponseMapper;
import com.example.banking.api.standingorders.schemas.StandingOrderResponseSchema;
import com.example.banking.api.standingorders.schemas.UpdateStandingOrderSchema;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.UpdateStandingOrderService;

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
public class UpdateStandingOrderRoute {
    private final UpdateStandingOrderService updateStandingOrderService;
    private final CustomerPrincipalResolver principalResolver;
    private final StandingOrderResponseMapper responseMapper;

    public UpdateStandingOrderRoute(
            UpdateStandingOrderService updateStandingOrderService,
            CustomerPrincipalResolver principalResolver,
            StandingOrderResponseMapper responseMapper) {
        this.updateStandingOrderService = updateStandingOrderService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Update standing order",
            description = "Updates editable standing order properties such as amount, schedule, or destination details.")
        @ApiResponse(
            responseCode = "200",
            description = "Standing order updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandingOrderResponseSchema.class),
                examples = @ExampleObject(value = "{\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"sourceAccountId\":\"a274560e-7158-41cb-8cc7-a305237b9f8c\",\"destinationAccountId\":\"a22f17f2-2122-4bb9-9e18-5f9dc9634df3\",\"amount\":\"150.00\",\"currencyCode\":\"AUD\",\"cadence\":\"MONTHLY\",\"lifecycleState\":\"ACTIVE\",\"nextExecutionAtUtc\":\"2026-08-01T00:00:00Z\"}")))
    @PatchMapping("/{standingOrderId}")
    public ResponseEntity<StandingOrderResponseSchema> update(
            @PathVariable String standingOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateStandingOrderSchema.class),
                    examples = @ExampleObject(value = "{\"amount\":150.00,\"cadence\":\"MONTHLY\",\"nextExecutionAtUtc\":\"2026-08-01T00:00:00Z\"}")))
            @Valid @RequestBody UpdateStandingOrderSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity updated = updateStandingOrderService.update(
                standingOrderId,
                request,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(updated));
    }
}
