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

    @PostMapping("/{standingOrderId}/pause")
    public ResponseEntity<StandingOrderResponseSchema> pause(
            @PathVariable String standingOrderId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity paused = lifecycleService.pause(standingOrderId, principal.userId(), principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(paused));
    }

    @PostMapping("/{standingOrderId}/resume")
    public ResponseEntity<StandingOrderResponseSchema> resume(
            @PathVariable String standingOrderId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity resumed = lifecycleService.resume(standingOrderId, principal.userId(), principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(resumed));
    }

    @PostMapping("/{standingOrderId}/cancel")
    public ResponseEntity<StandingOrderResponseSchema> cancel(
            @PathVariable String standingOrderId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity cancelled = lifecycleService.cancel(standingOrderId, principal.userId(), principal.role());
        return ResponseEntity.ok(responseMapper.toResponse(cancelled));
    }
}
