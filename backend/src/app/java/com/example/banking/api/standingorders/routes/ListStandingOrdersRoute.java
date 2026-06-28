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
