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

import jakarta.validation.Valid;

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

    @PatchMapping("/{standingOrderId}")
    public ResponseEntity<StandingOrderResponseSchema> update(
            @PathVariable String standingOrderId,
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
