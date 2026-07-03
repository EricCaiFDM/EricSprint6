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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.customer.dto.CreateCustomerRequest;
import com.example.banking.api.customer.dto.CustomerListResponse;
import com.example.banking.api.customer.dto.CustomerResponse;
import com.example.banking.api.customer.dto.DeleteCustomerResponse;
import com.example.banking.api.customer.dto.UpdateCustomerRequest;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.CustomerService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Customers")
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

    @Operation(
            summary = "Create customer",
            description = "Creates a new customer profile record and links ownership based on authenticated role and scope.")
        @ApiResponse(
            responseCode = "201",
            description = "Customer created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerResponse.class),
                examples = @ExampleObject(value = "{\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"externalCustomerKey\":\"EXT-1001\",\"legalName\":\"Jordan Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 412 345 678\",\"status\":\"ACTIVE\",\"createdAtUtc\":\"2026-07-02T02:01:00Z\",\"updatedAtUtc\":\"2026-07-02T02:01:00Z\",\"createdByUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\",\"ownerUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\"}")))
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateCustomerRequest.class),
                    examples = @ExampleObject(value = "{\"externalCustomerKey\":\"EXT-1001\",\"legalName\":\"Jordan Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 412 345 678\",\"password\":\"StrongPass123!\"}")))
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.createCustomer(request, principal.userId(), principal.role());
        return ResponseEntity.created(URI.create("/customers/" + response.customerId())).body(response);
    }

        @Operation(
            summary = "List customers",
            description = "Returns a paginated customer directory for authorized administrative access.")
        @ApiResponse(
            responseCode = "200",
            description = "Customer page",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerListResponse.class),
                examples = @ExampleObject(value = "{\"items\":[{\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"externalCustomerKey\":\"EXT-1001\",\"legalName\":\"Jordan Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 412 345 678\",\"status\":\"ACTIVE\",\"createdAtUtc\":\"2026-07-02T02:01:00Z\",\"updatedAtUtc\":\"2026-07-02T02:01:00Z\",\"createdByUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\",\"ownerUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\"}],\"page\":1,\"pageSize\":50,\"totalItems\":1,\"totalPages\":1}")))
    @GetMapping
    public ResponseEntity<CustomerListResponse> listCustomers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerListResponse response = customerService.listCustomers(page, pageSize, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

        @Operation(
            summary = "Get customer by id",
            description = "Returns a single customer profile by identifier when the caller is authorized for that customer.")
        @ApiResponse(
            responseCode = "200",
            description = "Customer details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerResponse.class),
                examples = @ExampleObject(value = "{\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"externalCustomerKey\":\"EXT-1001\",\"legalName\":\"Jordan Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 412 345 678\",\"status\":\"ACTIVE\",\"createdAtUtc\":\"2026-07-02T02:01:00Z\",\"updatedAtUtc\":\"2026-07-02T02:01:00Z\",\"createdByUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\",\"ownerUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\"}")))
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable String customerId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.getCustomerById(customerId, principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

        @Operation(
            summary = "Get current customer",
            description = "Resolves and returns the customer profile associated with the authenticated user identity.")
        @ApiResponse(
            responseCode = "200",
            description = "Current customer profile",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerResponse.class),
                examples = @ExampleObject(value = "{\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"externalCustomerKey\":\"EXT-1001\",\"legalName\":\"Jordan Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 412 345 678\",\"status\":\"ACTIVE\",\"createdAtUtc\":\"2026-07-02T02:01:00Z\",\"updatedAtUtc\":\"2026-07-02T02:01:00Z\",\"createdByUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\",\"ownerUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\"}")))
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getCurrentCustomer(Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        CustomerResponse response = customerService.getCurrentCustomer(principal.userId(), principal.role());
        return ResponseEntity.ok(response);
    }

        @Operation(
            summary = "Update customer",
            description = "Updates mutable customer profile fields such as legal name, contact information, or status.")
        @ApiResponse(
            responseCode = "200",
            description = "Customer updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerResponse.class),
                examples = @ExampleObject(value = "{\"customerId\":\"e8a95441-5b3f-4d4e-b294-58a1d5fc1f0b\",\"externalCustomerKey\":\"EXT-1001\",\"legalName\":\"Jordan T. Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 400 555 111\",\"status\":\"ACTIVE\",\"createdAtUtc\":\"2026-07-02T02:01:00Z\",\"updatedAtUtc\":\"2026-07-02T02:05:00Z\",\"createdByUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\",\"ownerUserId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\"}")))
    @PatchMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateCustomerRequest.class),
                    examples = @ExampleObject(value = "{\"legalName\":\"Jordan T. Patel\",\"primaryEmail\":\"jordan.patel@example.com\",\"phoneNumber\":\"+61 400 555 111\",\"status\":\"ACTIVE\"}")))
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

        @Operation(
            summary = "Delete customer",
            description = "Removes a customer from normal operational access subject to policy and authorization checks.")
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Customer removed",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeleteCustomerResponse.class),
                    examples = @ExampleObject(value = "{\"status\":\"DELETED\",\"message\":\"Customer removed from normal operational access\"}")))
        })
    @DeleteMapping("/{customerId}")
    public ResponseEntity<DeleteCustomerResponse> deleteCustomer(
            @PathVariable String customerId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        customerService.deleteCustomer(customerId, principal.userId(), principal.role());
        return ResponseEntity.ok(new DeleteCustomerResponse("DELETED", "Customer removed from normal operational access"));
    }
}
