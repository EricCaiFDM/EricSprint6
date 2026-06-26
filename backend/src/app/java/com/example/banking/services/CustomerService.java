package com.example.banking.services;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.customer.dto.CreateCustomerRequest;
import com.example.banking.api.customer.dto.CustomerListResponse;
import com.example.banking.api.customer.dto.CustomerResponse;
import com.example.banking.api.customer.dto.UpdateCustomerRequest;
import com.example.banking.models.CustomerEntity;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerFieldMaskingService maskingService;
    private final CustomerAccessPolicyService accessPolicyService;
    private final CustomerLifecycleAuditService lifecycleAuditService;
    private final CustomerDeletionPolicyService deletionPolicyService;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerFieldMaskingService maskingService,
            CustomerAccessPolicyService accessPolicyService,
            CustomerLifecycleAuditService lifecycleAuditService,
            CustomerDeletionPolicyService deletionPolicyService) {
        this.customerRepository = customerRepository;
        this.maskingService = maskingService;
        this.accessPolicyService = accessPolicyService;
        this.lifecycleAuditService = lifecycleAuditService;
        this.deletionPolicyService = deletionPolicyService;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            accessPolicyService.enforceCreateAccess(role);

            String normalizedExternalKey = normalizeRequired(request.externalCustomerKey(), "externalCustomerKey");
            String normalizedEmail = normalizeEmail(request.primaryEmail());
            ensureCreateUniqueness(normalizedExternalKey, normalizedEmail);

            Instant now = Instant.now();
            String customerId = UUID.randomUUID().toString();

            CustomerEntity entity = new CustomerEntity();
            entity.setCustomerId(customerId);
            entity.setExternalCustomerKey(normalizedExternalKey);
            entity.setLegalName(normalizeRequired(request.legalName(), "legalName"));
            entity.setPrimaryEmail(normalizedEmail);
            entity.setPhoneNumber(normalizeOptional(request.phoneNumber()));
            entity.setStatus("ACTIVE");
            entity.setCreatedAtUtc(now);
            entity.setUpdatedAtUtc(now);
            entity.setCreatedByUserId(actorId);
            entity.setOwnerUserId(actorId);
            entity.setDeletedAt(null);

            CustomerEntity saved = customerRepository.save(entity);
            lifecycleAuditService.recordSuccess(saved.getCustomerId(), "CREATE", actorId, role);
            return toResponse(saved, false, false);
        } catch (DataIntegrityViolationException exception) {
            lifecycleAuditService.recordFailure(null, "CREATE", actorId, role, "CUSTOMER_CONFLICT", "{}");
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "CUSTOMER_CONFLICT",
                    "Customer uniqueness constraint violated",
                    null);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(null, "CREATE", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public CustomerListResponse listCustomers(int page, int pageSize, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            accessPolicyService.enforceListAccess(role);

            int normalizedPage = Math.max(page, 1);
            int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));

            List<CustomerEntity> customers = customerRepository.findActiveCustomers();
            int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, customers.size());
            int toIndex = Math.min(fromIndex + normalizedPageSize, customers.size());

            List<CustomerResponse> items = customers.subList(fromIndex, toIndex).stream()
                    .map(customer -> toResponse(customer, false, false))
                    .toList();

            long totalItems = customers.size();
            int totalPages = (int) Math.ceil(totalItems / (double) normalizedPageSize);
            lifecycleAuditService.recordSuccess(null, "LIST", actorId, role);
            return new CustomerListResponse(items, normalizedPage, normalizedPageSize, totalItems, Math.max(totalPages, 1));
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(null, "LIST", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public CustomerResponse getCustomerById(String customerId, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            validateCustomerId(customerId);
            accessPolicyService.enforceReadAccess(role);

            CustomerEntity customer = activeCustomerOrThrow(customerId);
                accessPolicyService.enforceOwnershipIfRequired(
                    role,
                    actorId,
                    customer.getOwnerUserId(),
                    customer.getCreatedByUserId(),
                    "read");

            boolean applyMasking = "CUSTOMER".equalsIgnoreCase(role);
            lifecycleAuditService.recordSuccess(customer.getCustomerId(), "GET", actorId, role);
            return toResponse(customer, applyMasking, true);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(customerId, "GET", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public CustomerResponse getCurrentCustomer(String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            accessPolicyService.enforceReadAccess(role);

            CustomerEntity customer = customerRepository.findLatestActiveByOwnerUserId(actorId)
                .or(() -> customerRepository.findLatestActiveByCreatorUserId(actorId))
                    .orElseThrow(() -> new ApiErrorException(
                            HttpStatus.NOT_FOUND,
                            "CUSTOMER_NOT_FOUND",
                            "No customer account record found for this sign-in. Complete account setup first.",
                            null));

            accessPolicyService.enforceOwnershipIfRequired(
                role,
                actorId,
                customer.getOwnerUserId(),
                customer.getCreatedByUserId(),
                "read");

            if (!actorId.equals(customer.getOwnerUserId())) {
            customer.setOwnerUserId(actorId);
            customer.setUpdatedAtUtc(Instant.now());
            customer = customerRepository.save(customer);
            }

            boolean applyMasking = "CUSTOMER".equalsIgnoreCase(role);
            lifecycleAuditService.recordSuccess(customer.getCustomerId(), "GET", actorId, role);
            return toResponse(customer, applyMasking, true);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(null, "GET", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            validateCustomerId(customerId);
            accessPolicyService.enforceUpdateAccess(role);

            CustomerEntity customer = activeCustomerOrThrow(customerId);
                accessPolicyService.enforceOwnershipIfRequired(
                    role,
                    actorId,
                    customer.getOwnerUserId(),
                    customer.getCreatedByUserId(),
                    "update");

            if (!hasAnyPatchField(request)) {
                throw new ApiErrorException(
                        HttpStatus.BAD_REQUEST,
                        "CUSTOMER_VALIDATION_ERROR",
                        "At least one mutable field is required",
                        null);
            }

            if (request.legalName() != null) {
                customer.setLegalName(normalizeRequired(request.legalName(), "legalName"));
            }
            if (request.primaryEmail() != null) {
                String normalizedEmail = normalizeEmail(request.primaryEmail());
                if (!normalizedEmail.equalsIgnoreCase(customer.getPrimaryEmail())
                        && customerRepository.existsByPrimaryEmail(normalizedEmail)) {
                    throw new ApiErrorException(
                            HttpStatus.CONFLICT,
                            "CUSTOMER_CONFLICT",
                            "primaryEmail already exists",
                            "primaryEmail");
                }
                customer.setPrimaryEmail(normalizedEmail);
            }
            if (request.phoneNumber() != null) {
                customer.setPhoneNumber(normalizeOptional(request.phoneNumber()));
            }
            if (request.status() != null) {
                String nextStatus = request.status().trim().toUpperCase(Locale.ROOT);
                if (!isAllowedTransition(customer.getStatus(), nextStatus)) {
                    throw new ApiErrorException(
                            HttpStatus.BAD_REQUEST,
                            "CUSTOMER_VALIDATION_ERROR",
                            "Invalid status transition",
                            "status");
                }
                customer.setStatus(nextStatus);
            }

            customer.setUpdatedAtUtc(Instant.now());

            CustomerEntity saved = customerRepository.save(customer);
            lifecycleAuditService.recordSuccess(saved.getCustomerId(), "UPDATE", actorId, role);
            return toResponse(saved, false, false);
        } catch (DataIntegrityViolationException exception) {
            lifecycleAuditService.recordFailure(customerId, "UPDATE", actorId, role, "CUSTOMER_CONFLICT", "{}");
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "CUSTOMER_CONFLICT",
                    "Customer uniqueness constraint violated",
                    null);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(customerId, "UPDATE", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public void deleteCustomer(String customerId, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            validateCustomerId(customerId);
            accessPolicyService.enforceDeleteAccess(role);
            CustomerEntity customer = activeCustomerOrThrow(customerId);

                accessPolicyService.enforceOwnershipIfRequired(
                    role,
                    actorId,
                    customer.getOwnerUserId(),
                    customer.getCreatedByUserId(),
                    "delete");
            lifecycleAuditService.recordSuccess(customerId, "DELETE_ATTEMPT", actorId, role);

            CustomerDeletionPolicyService.DeletionDecision decision = deletionPolicyService.evaluateDeletion(customer);
            if ("BLOCK_DELETE".equals(decision.decision())) {
                lifecycleAuditService.recordFailure(
                        customerId,
                        "DELETE_BLOCKED",
                        actorId,
                        role,
                        "DELETION_POLICY_BLOCKED",
                        "{\"reasons\":\"" + String.join(",", decision.blockerReasons()) + "\"}");
            }
            deletionPolicyService.enforceDeletionAllowed(decision);

            customer.setDeletedAt(Instant.now());
            customer.setUpdatedAtUtc(Instant.now());
            customerRepository.save(customer);

            lifecycleAuditService.recordSuccess(customerId, "DELETE_SUCCESS", actorId, role);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(customerId, "DELETE_ATTEMPT", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    private CustomerEntity activeCustomerOrThrow(String customerId) {
        Optional<CustomerEntity> candidate = customerRepository.findActiveById(customerId);
        if (candidate.isEmpty()) {
            throw new ApiErrorException(
                    HttpStatus.NOT_FOUND,
                    "CUSTOMER_NOT_FOUND",
                    "No customer found with the provided customerId",
                    "customerId");
        }
        return candidate.get();
    }

    private void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new ApiErrorException(
                    HttpStatus.BAD_REQUEST,
                    "CUSTOMER_VALIDATION_ERROR",
                    "customerId is required",
                    "customerId");
        }

        try {
            UUID.fromString(customerId);
        } catch (IllegalArgumentException exception) {
            throw new ApiErrorException(
                    HttpStatus.BAD_REQUEST,
                    "CUSTOMER_VALIDATION_ERROR",
                    "customerId must be a UUID",
                    "customerId");
        }
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiErrorException(
                    HttpStatus.BAD_REQUEST,
                    "CUSTOMER_VALIDATION_ERROR",
                    field + " is required",
                    field);
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email, "primaryEmail").toLowerCase(Locale.ROOT);
    }

    private void ensureCreateUniqueness(String externalKey, String email) {
        if (customerRepository.existsByExternalCustomerKey(externalKey)) {
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "CUSTOMER_CONFLICT",
                    "externalCustomerKey already exists",
                    "externalCustomerKey");
        }

        if (customerRepository.existsByPrimaryEmail(email)) {
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "CUSTOMER_CONFLICT",
                    "primaryEmail already exists",
                    "primaryEmail");
        }
    }

    private boolean hasAnyPatchField(UpdateCustomerRequest request) {
        return request.legalName() != null
                || request.primaryEmail() != null
                || request.phoneNumber() != null
                || request.status() != null;
    }

    private boolean isAllowedTransition(String currentStatus, String nextStatus) {
        String current = currentStatus == null ? "ACTIVE" : currentStatus.toUpperCase(Locale.ROOT);
        if (current.equals(nextStatus)) {
            return true;
        }
        return ("ACTIVE".equals(current) && "SUSPENDED".equals(nextStatus))
                || ("SUSPENDED".equals(current) && "ACTIVE".equals(nextStatus))
                || (("ACTIVE".equals(current) || "SUSPENDED".equals(current)) && "CLOSED".equals(nextStatus));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId;
    }

    private CustomerResponse toResponse(CustomerEntity entity, boolean masked, boolean readOperation) {
        String primaryEmail = masked ? maskingService.maskEmail(entity.getPrimaryEmail()) : entity.getPrimaryEmail();
        String phoneNumber = masked ? maskingService.maskPhone(entity.getPhoneNumber()) : entity.getPhoneNumber();

        if (readOperation && masked && phoneNumber == null) {
            phoneNumber = "***-***-****";
        }

        return new CustomerResponse(
                entity.getCustomerId(),
                entity.getExternalCustomerKey(),
                entity.getLegalName(),
                primaryEmail,
                phoneNumber,
                entity.getStatus(),
                entity.getCreatedAtUtc().toString(),
                entity.getUpdatedAtUtc().toString(),
                entity.getCreatedByUserId(),
                entity.getOwnerUserId());
    }
}
