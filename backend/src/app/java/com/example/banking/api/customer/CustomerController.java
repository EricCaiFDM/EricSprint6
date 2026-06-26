package com.example.banking.api.customer;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.customer.dto.CreateCustomerRequest;
import com.example.banking.api.customer.dto.CustomerResponse;
import com.example.banking.api.customer.dto.DeleteCustomerResponse;
import com.example.banking.api.customer.dto.UpdateCustomerRequest;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/customers")
@Validated
public class CustomerController {
    private final CustomerService customerService;
    private final CustomerPrincipalResolver principalResolver;

    public CustomerController(CustomerService customerService, CustomerPrincipalResolver principalResolver) {
        this.customerService = customerService;
        this.principalResolver = principalResolver;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.createCustomer(request, principal.userId(), principal.role());
        return ResponseEntity.created(URI.create("/customers/" + response.customerId())).body(response);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable String customerId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.getCustomerById(customerId, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getCurrentCustomer(Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.getCurrentCustomer(principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.updateCustomer(
                customerId,
                request,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<DeleteCustomerResponse> deleteCustomer(
            @PathVariable String customerId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        customerService.deleteCustomer(customerId, principal.userId(), principal.role());
        return ResponseEntity.ok(new DeleteCustomerResponse("DELETED", "Customer removed from normal operational access"));
    }
}
