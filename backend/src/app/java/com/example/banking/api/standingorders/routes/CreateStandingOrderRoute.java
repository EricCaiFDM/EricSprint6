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

import jakarta.validation.Valid;

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

    @PostMapping
    public ResponseEntity<StandingOrderResponseSchema> create(
            @Valid @RequestBody CreateStandingOrderSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        StandingOrderEntity created = createStandingOrderService.create(request, principal.userId(), principal.role());
        StandingOrderResponseSchema response = responseMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/standing-orders/" + response.standingOrderId())).body(response);
    }
}
