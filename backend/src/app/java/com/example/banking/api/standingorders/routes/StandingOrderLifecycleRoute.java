package com.example.banking.api.standingorders.routes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.standingorders.StandingOrderResponseMapper;
import com.example.banking.api.standingorders.schemas.StandingOrderResponseSchema;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.StandingOrderLifecycleService;

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
public class StandingOrderLifecycleRoute {
    private final StandingOrderLifecycleService lifecycleService;
    private final CustomerPrincipalResolver principalResolver;
    private final StandingOrderResponseMapper responseMapper;

    public StandingOrderLifecycleRoute(
            StandingOrderLifecycleService lifecycleService,
            CustomerPrincipalResolver principalResolver,
            StandingOrderResponseMapper responseMapper) {
        this.lifecycleService = lifecycleService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Pause standing order",
            description = "Transitions an active standing order into a paused state to stop future automatic executions.")
        @ApiResponse(
            responseCode = "200",
            description = "Standing order paused",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandingOrderResponseSchema.class),
                examples = @ExampleObject(value = "{\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"lifecycleState\":\"PAUSED\",\"nextExecutionAtUtc\":\"2026-08-01T00:00:00Z\"}")))
    @PostMapping("/{standingOrderId}/pause")
    public ResponseEntity<StandingOrderResponseSchema> pause(
            @PathVariable String standingOrderId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity paused = lifecycleService.pause(standingOrderId, principal.userId(), principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(paused));
    }

        @Operation(
            summary = "Resume standing order",
            description = "Reactivates a paused standing order so scheduled executions continue.")
        @ApiResponse(
            responseCode = "200",
            description = "Standing order resumed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandingOrderResponseSchema.class),
                examples = @ExampleObject(value = "{\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"lifecycleState\":\"ACTIVE\",\"nextExecutionAtUtc\":\"2026-08-01T00:00:00Z\"}")))
    @PostMapping("/{standingOrderId}/resume")
    public ResponseEntity<StandingOrderResponseSchema> resume(
            @PathVariable String standingOrderId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity resumed = lifecycleService.resume(standingOrderId, principal.userId(), principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(resumed));
    }

        @Operation(
            summary = "Cancel standing order",
            description = "Cancels a standing order and prevents future executions under that instruction.")
        @ApiResponse(
            responseCode = "200",
            description = "Standing order cancelled",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandingOrderResponseSchema.class),
                examples = @ExampleObject(value = "{\"standingOrderId\":\"c1d2e3f4-1111-4444-9999-aabbccddeeff\",\"lifecycleState\":\"CANCELLED\",\"nextExecutionAtUtc\":\"N/A\"}")))
    @PostMapping("/{standingOrderId}/cancel")
    public ResponseEntity<StandingOrderResponseSchema> cancel(
            @PathVariable String standingOrderId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity cancelled = lifecycleService.cancel(standingOrderId, principal.userId(), principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(cancelled));
    }
}
