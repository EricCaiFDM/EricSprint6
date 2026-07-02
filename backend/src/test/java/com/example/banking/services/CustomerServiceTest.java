package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.customer.dto.CreateCustomerRequest;
import com.example.banking.api.customer.dto.CustomerListResponse;
import com.example.banking.api.customer.dto.CustomerResponse;
import com.example.banking.api.customer.dto.UpdateCustomerRequest;
import com.example.banking.lib.config.CustomerModuleConfig;
import com.example.banking.models.CustomerEntity;

class CustomerServiceTest {

    private InMemoryCustomerRepository customerRepository;
    private CapturingAccessPolicyService accessPolicyService;
    private CapturingLifecycleAuditService lifecycleAuditService;
    private CapturingDeletionPolicyService deletionPolicyService;
    private CapturingAuthService authService;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customerRepository = new InMemoryCustomerRepository();
        accessPolicyService = new CapturingAccessPolicyService();
        lifecycleAuditService = new CapturingLifecycleAuditService();
        deletionPolicyService = new CapturingDeletionPolicyService();
        authService = new CapturingAuthService();

        CustomerModuleConfig moduleConfig = new CustomerModuleConfig();
        moduleConfig.setMaskingEnabled(true);
        CustomerFieldMaskingService maskingService = new CustomerFieldMaskingService(moduleConfig);

        service = new CustomerService(
                customerRepository,
                maskingService,
                accessPolicyService,
                lifecycleAuditService,
                deletionPolicyService,
                authService);
    }

    @Test
    void createCustomerNonAdminSuccessUsesActorAsOwner() {
        CustomerResponse response = service.createCustomer(
                createRequest(" ext-001 ", " Jane Doe ", "User@Example.Com", " 123-456-7890 ", null),
                "actor-1",
                "CUSTOMER");

        assertNotNull(response.customerId());
        assertEquals("ext-001", response.externalCustomerKey());
        assertEquals("Jane Doe", response.legalName());
        assertEquals("user@example.com", response.primaryEmail());
        assertEquals("123-456-7890", response.phoneNumber());
        assertEquals("actor-1", response.ownerUserId());
        assertEquals(0, authService.registerCalls.size());

        assertEquals(1, lifecycleAuditService.successCalls.size());
        assertEquals("CREATE", lifecycleAuditService.successCalls.get(0).eventType());
    }

    @Test
    void createCustomerAdminSuccessProvisionsOwnerAndNormalizesAnonymousActor() {
        UUID provisionedUserId = UUID.randomUUID();
        authService.nextUserId = provisionedUserId;

        CustomerResponse response = service.createCustomer(
                createRequest("ext-002", "Admin Created", "ADMIN@EXAMPLE.COM", null, "Password#123"),
                null,
                "ADMIN");

        assertEquals(provisionedUserId.toString(), response.ownerUserId());
        assertEquals("anonymous", response.createdByUserId());
        assertEquals("admin@example.com", authService.registerCalls.get(0).email());
        assertEquals("Password#123", authService.registerCalls.get(0).password());
    }

    @Test
    void createCustomerAdminPasswordValidationCoversNullBlankShortAndLong() {
        ApiErrorException nullPassword = captureCreateCustomerError(
            createRequest("ext-003", "Null Password", "n1@example.com", null, null),
            "actor",
            "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", nullPassword.getCode());
        assertEquals("password", nullPassword.getField());

        ApiErrorException blankPassword = captureCreateCustomerError(
            createRequest("ext-004", "Blank Password", "n2@example.com", null, "  "),
            "actor",
            "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", blankPassword.getCode());
        assertEquals("password", blankPassword.getField());

        ApiErrorException shortPassword = captureCreateCustomerError(
            createRequest("ext-005", "Short Password", "n3@example.com", null, "1234567"),
            "actor",
            "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", shortPassword.getCode());
        assertEquals("password", shortPassword.getField());

        String longPassword = "a".repeat(129);
        ApiErrorException longPasswordError = captureCreateCustomerError(
            createRequest("ext-006", "Long Password", "n4@example.com", null, longPassword),
            "actor",
            "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", longPasswordError.getCode());
        assertEquals("password", longPasswordError.getField());
    }

    @Test
    void createCustomerAdminAuthProvisionExceptionsAreMapped() {
        authService.illegalState = new IllegalStateException("duplicate identity");
        ApiErrorException conflict = captureCreateCustomerError(
            createRequest("ext-007", "Conflict", "dup@example.com", null, "Password#123"),
            "actor",
            "ADMIN");
        assertEquals("CUSTOMER_CONFLICT", conflict.getCode());
        assertEquals("primaryEmail", conflict.getField());

        authService.illegalState = null;
        authService.illegalArgument = new IllegalArgumentException("policy violation");
        ApiErrorException validation = captureCreateCustomerError(
            createRequest("ext-008", "Validation", "bad@example.com", null, "Password#123"),
            "actor",
            "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", validation.getCode());
        assertEquals("password", validation.getField());
    }

    @Test
    void createCustomerUniquenessAndDataIntegrityConflictsAreMapped() {
        customerRepository.externalKeys.add("dup-ext");
        ApiErrorException externalConflict = captureCreateCustomerError(
            createRequest("dup-ext", "Dup Ext", "ext@example.com", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("CUSTOMER_CONFLICT", externalConflict.getCode());
        assertEquals("externalCustomerKey", externalConflict.getField());

        customerRepository.externalKeys.clear();
        customerRepository.primaryEmails.add("dup@example.com");
        ApiErrorException emailConflict = captureCreateCustomerError(
            createRequest("ext-009", "Dup Email", "DUP@EXAMPLE.COM", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("CUSTOMER_CONFLICT", emailConflict.getCode());
        assertEquals("primaryEmail", emailConflict.getField());

        customerRepository.primaryEmails.clear();
        customerRepository.throwDataIntegrityOnSave = true;
        ApiErrorException saveConflict = captureCreateCustomerError(
            createRequest("ext-010", "Save Conflict", "save@example.com", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("CUSTOMER_CONFLICT", saveConflict.getCode());
        assertNull(saveConflict.getField());
    }

    @Test
    void createCustomerRequiredFieldValidationCoversNullAndBlank() {
        ApiErrorException nullLegalName = captureCreateCustomerError(
            createRequest("ext-011", null, "null-name@example.com", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("CUSTOMER_VALIDATION_ERROR", nullLegalName.getCode());
        assertEquals("legalName", nullLegalName.getField());

        ApiErrorException blankLegalName = captureCreateCustomerError(
            createRequest("ext-012", "   ", "blank-name@example.com", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("CUSTOMER_VALIDATION_ERROR", blankLegalName.getCode());
        assertEquals("legalName", blankLegalName.getField());
    }

    @Test
    void listCustomersNormalizesPageAndSizeAndUsesMinimumTotalPagesOfOne() {
        customerRepository.activeCustomers.clear();

        CustomerListResponse response = service.listCustomers(0, 0, "  ", "ADMIN");

        assertEquals(1, response.page());
        assertEquals(1, response.pageSize());
        assertEquals(0, response.totalItems());
        assertEquals(1, response.totalPages());
        assertTrue(response.items().isEmpty());

        assertEquals("anonymous", lifecycleAuditService.successCalls.get(0).actorUserId());
    }

    @Test
    void listCustomersReturnsRequestedSliceAndTotalPages() {
        customerRepository.activeCustomers = new ArrayList<>(List.of(
                customer(UUID.randomUUID().toString(), "ext-a", "Alice", "a@example.com", "111", "ACTIVE", "owner-a", "creator-a"),
                customer(UUID.randomUUID().toString(), "ext-b", "Bob", "b@example.com", "222", "ACTIVE", "owner-b", "creator-b"),
                customer(UUID.randomUUID().toString(), "ext-c", "Carol", "c@example.com", "333", "ACTIVE", "owner-c", "creator-c")));

        CustomerListResponse response = service.listCustomers(2, 2, "actor", "ADMIN");

        assertEquals(2, response.page());
        assertEquals(2, response.pageSize());
        assertEquals(3, response.totalItems());
        assertEquals(2, response.totalPages());
        assertEquals(1, response.items().size());
        assertEquals("ext-c", response.items().get(0).externalCustomerKey());
    }

    @Test
    void listCustomersRecordsFailureWhenAccessDenied() {
        accessPolicyService.listException = forbidden("list");

        ApiErrorException exception = captureListCustomersError(1, 20, "actor", "CUSTOMER");

        assertEquals("CUSTOMER_FORBIDDEN", exception.getCode());
        assertEquals("LIST", lifecycleAuditService.failureCalls.get(0).eventType());
    }

    @Test
    void getCustomerByIdAppliesMaskingAndPhonePlaceholderForCustomerRead() {
        String id = UUID.randomUUID().toString();
        CustomerEntity entity = customer(id, "ext-013", "Masked", "masked@example.com", null, "ACTIVE", "owner", "creator");
        customerRepository.byId.put(id, entity);

        CustomerResponse response = service.getCustomerById(id, "owner", "CUSTOMER");

        assertEquals("m***@example.com", response.primaryEmail());
        assertEquals("***-***-****", response.phoneNumber());
    }

    @Test
    void getCustomerByIdReturnsUnmaskedForAdmin() {
        String id = UUID.randomUUID().toString();
        CustomerEntity entity = customer(id, "ext-014", "Admin", "admin@example.com", "5551234567", "ACTIVE", "owner", "creator");
        customerRepository.byId.put(id, entity);

        CustomerResponse response = service.getCustomerById(id, "admin", "ADMIN");

        assertEquals("admin@example.com", response.primaryEmail());
        assertEquals("5551234567", response.phoneNumber());
    }

    @Test
    void getCustomerByIdValidationAndLookupFailuresAreHandled() {
        ApiErrorException blankId = captureGetCustomerByIdError(" ", "actor", "CUSTOMER");
        assertEquals("CUSTOMER_VALIDATION_ERROR", blankId.getCode());

        ApiErrorException invalidId = captureGetCustomerByIdError("not-a-uuid", "actor", "CUSTOMER");
        assertEquals("CUSTOMER_VALIDATION_ERROR", invalidId.getCode());

        String missingId = UUID.randomUUID().toString();
        ApiErrorException notFound = captureGetCustomerByIdError(missingId, "actor", "CUSTOMER");
        assertEquals("CUSTOMER_NOT_FOUND", notFound.getCode());

        String existingId = UUID.randomUUID().toString();
        customerRepository.byId.put(existingId, customer(existingId, "ext-015", "Owned", "owned@example.com", null, "ACTIVE", "owner", "creator"));
        accessPolicyService.ownershipException = forbidden("read");
        ApiErrorException forbidden = captureGetCustomerByIdError(existingId, "other", "CUSTOMER");
        assertEquals("CUSTOMER_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void getCurrentCustomerByOwnerHasNoReassignmentSave() {
        CustomerEntity ownerCustomer = customer(
                UUID.randomUUID().toString(),
                "ext-016",
                "Owner",
                "owner@example.com",
                "7771234567",
                "ACTIVE",
                "actor-1",
                "creator-1");
        customerRepository.ownerLookup = Optional.of(ownerCustomer);

        CustomerResponse response = service.getCurrentCustomer("actor-1", "ADMIN");

        assertEquals(ownerCustomer.getCustomerId(), response.customerId());
        assertEquals(0, customerRepository.saved.size());
    }

    @Test
    void getCurrentCustomerFallsBackToCreatorAndReassignsOwner() {
        CustomerEntity creatorCustomer = customer(
                UUID.randomUUID().toString(),
                "ext-017",
                "Creator",
                "creator@example.com",
                null,
                "ACTIVE",
                "old-owner",
                "actor-2");
        customerRepository.ownerLookup = Optional.empty();
        customerRepository.creatorLookup = Optional.of(creatorCustomer);

        CustomerResponse response = service.getCurrentCustomer("actor-2", "CUSTOMER");

        assertEquals("actor-2", response.ownerUserId());
        assertEquals(1, customerRepository.saved.size());
        assertEquals("c***@example.com", response.primaryEmail());
        assertEquals("***-***-****", response.phoneNumber());
    }

    @Test
    void getCurrentCustomerNotFoundAndForbiddenBranchesAreHandled() {
        customerRepository.ownerLookup = Optional.empty();
        customerRepository.creatorLookup = Optional.empty();

        ApiErrorException notFound = captureGetCurrentCustomerError("actor-3", "CUSTOMER");
        assertEquals("CUSTOMER_NOT_FOUND", notFound.getCode());

        accessPolicyService.readException = forbidden("read");
        ApiErrorException forbidden = captureGetCurrentCustomerError("actor-3", "CUSTOMER");
        assertEquals("CUSTOMER_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void updateCustomerValidatesCustomerId() {
        UpdateCustomerRequest request = new UpdateCustomerRequest("Legal", null, null, null);

        ApiErrorException missing = captureUpdateCustomerError(null, request, "actor", "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", missing.getCode());

        ApiErrorException invalid = captureUpdateCustomerError("not-a-uuid", request, "actor", "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", invalid.getCode());
    }

    @Test
    void updateCustomerRejectsWhenNoPatchFieldProvided() {
        String id = UUID.randomUUID().toString();
        customerRepository.byId.put(id, customer(id, "ext-018", "No Patch", "np@example.com", null, "ACTIVE", "owner", "creator"));

        ApiErrorException exception = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest(null, null, null, null),
            "owner",
            "CUSTOMER");

        assertEquals("CUSTOMER_VALIDATION_ERROR", exception.getCode());
    }

    @Test
    void updateCustomerHandlesLegalNameAndPhoneNormalizationBranches() {
        String id = UUID.randomUUID().toString();
        customerRepository.byId.put(id, customer(id, "ext-019", "Old Legal", "old@example.com", "5551111111", "ACTIVE", "owner", "creator"));

        CustomerResponse legalOnly = service.updateCustomer(
                id,
                new UpdateCustomerRequest("  New Legal  ", null, null, null),
                "owner",
                "CUSTOMER");
        assertEquals("New Legal", legalOnly.legalName());

        CustomerResponse phoneOnly = service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, " 5552223333 ", null),
                "owner",
                "CUSTOMER");
        assertEquals("5552223333", phoneOnly.phoneNumber());

        CustomerResponse blankPhone = service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, "   ", null),
                "owner",
                "CUSTOMER");
        assertNull(blankPhone.phoneNumber());
    }

    @Test
    void updateCustomerEmailBranchesCoverSameChangedConflictAndUnique() {
        String id = UUID.randomUUID().toString();
        customerRepository.byId.put(id, customer(id, "ext-020", "Email", "same@example.com", null, "ACTIVE", "owner", "creator"));

        CustomerResponse sameEmail = service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, "SAME@EXAMPLE.COM", null, null),
                "owner",
                "CUSTOMER");
        assertEquals("same@example.com", sameEmail.primaryEmail());
        assertEquals(0, authService.updateEmailCalls.size());

        customerRepository.primaryEmails.add("taken@example.com");
        ApiErrorException conflict = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest(null, "taken@example.com", null, null),
            "owner",
            "CUSTOMER");
        assertEquals("CUSTOMER_CONFLICT", conflict.getCode());
        assertEquals("primaryEmail", conflict.getField());

        customerRepository.primaryEmails.remove("taken@example.com");
        CustomerResponse uniqueEmail = service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, "new@example.com", null, null),
                "owner",
                "CUSTOMER");
        assertEquals("new@example.com", uniqueEmail.primaryEmail());
        assertEquals(1, authService.updateEmailCalls.size());
        assertEquals("owner", authService.updateEmailCalls.get(0).userId());
        assertEquals("new@example.com", authService.updateEmailCalls.get(0).email());
    }

    @Test
    void updateCustomerMapsAuthIdentityEmailConflict() {
        String id = UUID.randomUUID().toString();
        customerRepository.byId.put(id, customer(id, "ext-020b", "Email", "same2@example.com", null, "ACTIVE", "owner", "creator"));
        authService.emailUpdateIllegalState = new IllegalStateException("Email already registered with role CUSTOMER");

        ApiErrorException conflict = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest(null, "taken@example.com", null, null),
            "owner",
            "CUSTOMER");

        assertEquals("CUSTOMER_CONFLICT", conflict.getCode());
        assertEquals("primaryEmail", conflict.getField());
    }

    @Test
    void updateCustomerFailsWhenAuthIdentityRecordCannotBeUpdated() {
        String id = UUID.randomUUID().toString();
        customerRepository.byId.put(id, customer(id, "ext-020c", "Email", "same3@example.com", null, "ACTIVE", "owner", "creator"));
        authService.nextUpdateEmailResult = false;

        ApiErrorException conflict = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest(null, "new3@example.com", null, null),
            "owner",
            "CUSTOMER");

        assertEquals("CUSTOMER_CONFLICT", conflict.getCode());
        assertEquals("primaryEmail", conflict.getField());
    }

    @Test
    void updateCustomerStatusTransitionBranchesAreCovered() {
        String id = UUID.randomUUID().toString();
        CustomerEntity entity = customer(id, "ext-021", "Status", "status@example.com", null, null, "owner", "creator");
        customerRepository.byId.put(id, entity);

        CustomerResponse currentNullDefaultsToActive = service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, null, "ACTIVE"),
                "owner",
                "CUSTOMER");
        assertEquals("ACTIVE", currentNullDefaultsToActive.status());

        entity.setStatus("ACTIVE");
        assertEquals("SUSPENDED", service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, null, "SUSPENDED"),
                "owner",
                "CUSTOMER").status());

        entity.setStatus("SUSPENDED");
        assertEquals("ACTIVE", service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, null, "ACTIVE"),
                "owner",
                "CUSTOMER").status());

        entity.setStatus("ACTIVE");
        assertEquals("CLOSED", service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, null, "CLOSED"),
                "owner",
                "CUSTOMER").status());

        entity.setStatus("SUSPENDED");
        assertEquals("CLOSED", service.updateCustomer(
                id,
                new UpdateCustomerRequest(null, null, null, "CLOSED"),
                "owner",
                "CUSTOMER").status());

        entity.setStatus("CLOSED");
        ApiErrorException invalidTransition = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest(null, null, null, "ACTIVE"),
            "owner",
            "CUSTOMER");
        assertEquals("CUSTOMER_VALIDATION_ERROR", invalidTransition.getCode());
        assertEquals("status", invalidTransition.getField());

                entity.setStatus("ACTIVE");
            ApiErrorException unsupportedTransition = captureUpdateCustomerError(
                id,
                new UpdateCustomerRequest(null, null, null, "PAUSED"),
                "owner",
                "CUSTOMER");
                assertEquals("CUSTOMER_VALIDATION_ERROR", unsupportedTransition.getCode());
                assertEquals("status", unsupportedTransition.getField());
    }

    @Test
    void updateCustomerConflictAndAccessAndOwnershipFailuresAreMapped() {
        String id = UUID.randomUUID().toString();
        customerRepository.byId.put(id, customer(id, "ext-022", "Update", "up@example.com", null, "ACTIVE", "owner", "creator"));

        customerRepository.throwDataIntegrityOnSave = true;
        ApiErrorException conflict = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest("Name", null, null, null),
            "owner",
            "CUSTOMER");
        assertEquals("CUSTOMER_CONFLICT", conflict.getCode());
        customerRepository.throwDataIntegrityOnSave = false;

        accessPolicyService.updateException = forbidden("update");
        ApiErrorException accessForbidden = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest("Name", null, null, null),
            "owner",
            "CUSTOMER");
        assertEquals("CUSTOMER_FORBIDDEN", accessForbidden.getCode());
        accessPolicyService.updateException = null;

        accessPolicyService.ownershipException = forbidden("update");
        ApiErrorException ownershipForbidden = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest("Name", null, null, null),
            "owner",
            "CUSTOMER");
        assertEquals("CUSTOMER_FORBIDDEN", ownershipForbidden.getCode());
        accessPolicyService.ownershipException = null;

        customerRepository.byId.clear();
        ApiErrorException notFound = captureUpdateCustomerError(
            id,
            new UpdateCustomerRequest("Name", null, null, null),
            "owner",
            "CUSTOMER");
        assertEquals("CUSTOMER_NOT_FOUND", notFound.getCode());
    }

    @Test
    void deleteCustomerAllowDecisionMarksDeletedAndRecordsSuccess() {
        String id = UUID.randomUUID().toString();
        CustomerEntity entity = customer(id, "ext-023", "Delete", "delete@example.com", null, "ACTIVE", "owner", "creator");
        customerRepository.byId.put(id, entity);
        deletionPolicyService.nextDecision = new CustomerDeletionPolicyService.DeletionDecision(false, false, List.of(), "ALLOW_DELETE");

        service.deleteCustomer(id, "owner", "ADMIN");

        assertNotNull(entity.getDeletedAt());
        assertEquals("closed-" + id, entity.getExternalCustomerKey());
        assertEquals("closed+" + id + "@customer.local", entity.getPrimaryEmail());
        assertEquals(1, authService.deactivateIdentityCalls.size());
        assertEquals("owner", authService.deactivateIdentityCalls.get(0));
        assertEquals(1, lifecycleAuditService.successCalls.stream().filter(call -> "DELETE_SUCCESS".equals(call.eventType())).count());
    }

    @Test
    void deleteCustomerBlockedForAdminCanThrowOrProceedDependingOnEnforcer() {
        String id = UUID.randomUUID().toString();
        CustomerEntity entity = customer(id, "ext-024", "Blocked", "blocked@example.com", null, "ACTIVE", "owner", "creator");
        customerRepository.byId.put(id, entity);
        deletionPolicyService.nextDecision = new CustomerDeletionPolicyService.DeletionDecision(true, false, List.of("DEPENDENCY_BLOCKER"), "BLOCK_DELETE");

        deletionPolicyService.enforceException = new ApiErrorException(HttpStatus.CONFLICT, "CUSTOMER_DELETE_BLOCKED", "blocked", null);
        ApiErrorException blocked = captureDeleteCustomerError(id, "owner", "ADMIN");
        assertEquals("CUSTOMER_DELETE_BLOCKED", blocked.getCode());

        deletionPolicyService.enforceException = null;
        entity.setDeletedAt(null);
        assertNull(captureDeleteCustomerError(id, "owner", "ADMIN"));
        assertNotNull(entity.getDeletedAt());
    }

    @Test
    void deleteCustomerBlockedForCustomerUsesOverridePath() {
        String id = UUID.randomUUID().toString();
        CustomerEntity entity = customer(id, "ext-025", "Override", "override@example.com", null, "ACTIVE", "owner", "creator");
        customerRepository.byId.put(id, entity);
        deletionPolicyService.nextDecision = new CustomerDeletionPolicyService.DeletionDecision(true, false, List.of("DEPENDENCY_BLOCKER"), "BLOCK_DELETE");

        service.deleteCustomer(id, "owner", "CUSTOMER");

        assertNotNull(entity.getDeletedAt());
        assertEquals("closed-" + id, entity.getExternalCustomerKey());
        assertEquals("closed+" + id + "@customer.local", entity.getPrimaryEmail());
        assertEquals(1, authService.deactivateIdentityCalls.size());
        assertEquals("owner", authService.deactivateIdentityCalls.get(0));
        assertTrue(lifecycleAuditService.successCalls.stream().anyMatch(call -> "DELETE_POLICY_OVERRIDE".equals(call.eventType())));
    }

    @Test
    void deleteCustomerValidationAndAccessFailuresAreHandled() {
        ApiErrorException blankId = captureDeleteCustomerError(" ", "owner", "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", blankId.getCode());

        ApiErrorException invalidId = captureDeleteCustomerError("not-a-uuid", "owner", "ADMIN");
        assertEquals("CUSTOMER_VALIDATION_ERROR", invalidId.getCode());

        String id = UUID.randomUUID().toString();
        accessPolicyService.deleteException = forbidden("delete");
        ApiErrorException accessDenied = captureDeleteCustomerError(id, "owner", "CUSTOMER");
        assertEquals("CUSTOMER_FORBIDDEN", accessDenied.getCode());
        accessPolicyService.deleteException = null;

        ApiErrorException notFound = captureDeleteCustomerError(id, "owner", "ADMIN");
        assertEquals("CUSTOMER_NOT_FOUND", notFound.getCode());

        customerRepository.byId.put(id, customer(id, "ext-026", "Owned", "owned@example.com", null, "ACTIVE", "owner", "creator"));
        accessPolicyService.ownershipException = forbidden("delete");
        ApiErrorException ownershipDenied = captureDeleteCustomerError(id, "other", "CUSTOMER");
        assertEquals("CUSTOMER_FORBIDDEN", ownershipDenied.getCode());
    }

    private CreateCustomerRequest createRequest(
            String externalCustomerKey,
            String legalName,
            String primaryEmail,
            String phoneNumber,
            String password) {
        return new CreateCustomerRequest(externalCustomerKey, legalName, primaryEmail, phoneNumber, password);
    }

    private CustomerEntity customer(
            String customerId,
            String externalKey,
            String legalName,
            String email,
            String phone,
            String status,
            String ownerUserId,
            String createdByUserId) {
        CustomerEntity entity = new CustomerEntity();
        entity.setCustomerId(customerId);
        entity.setExternalCustomerKey(externalKey);
        entity.setLegalName(legalName);
        entity.setPrimaryEmail(email);
        entity.setPhoneNumber(phone);
        entity.setStatus(status);
        entity.setCreatedAtUtc(Instant.parse("2026-06-30T00:00:00Z"));
        entity.setUpdatedAtUtc(Instant.parse("2026-06-30T00:00:00Z"));
        entity.setOwnerUserId(ownerUserId);
        entity.setCreatedByUserId(createdByUserId);
        return entity;
    }

    private ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "CUSTOMER_FORBIDDEN",
                "Insufficient privileges to " + operation + " customer profile",
                null);
    }

    private ApiErrorException captureCreateCustomerError(
            CreateCustomerRequest request,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.createCustomer(request, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureListCustomersError(int page, int pageSize, String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.listCustomers(page, pageSize, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureGetCustomerByIdError(String customerId, String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.getCustomerById(customerId, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureGetCurrentCustomerError(String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.getCurrentCustomer(actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureUpdateCustomerError(
            String customerId,
            UpdateCustomerRequest request,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.updateCustomer(customerId, request, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureDeleteCustomerError(String customerId, String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.deleteCustomer(customerId, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private record RegisterCall(String email, String password, String passwordConfirmation, String role) {
    }

    private record EmailUpdateCall(String userId, String email) {
    }

    private record SuccessAuditCall(String customerId, String eventType, String actorUserId, String actorRole) {
    }

    private record FailureAuditCall(
            String customerId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<String, CustomerEntity> byId = new HashMap<>();
        private List<CustomerEntity> activeCustomers = new ArrayList<>();
        private Optional<CustomerEntity> ownerLookup = Optional.empty();
        private Optional<CustomerEntity> creatorLookup = Optional.empty();
        private final Set<String> externalKeys = new HashSet<>();
        private final Set<String> primaryEmails = new HashSet<>();
        private final List<CustomerEntity> saved = new ArrayList<>();
        private boolean throwDataIntegrityOnSave;

        @Override
        public CustomerEntity save(CustomerEntity customer) {
            if (throwDataIntegrityOnSave) {
                throw new DataIntegrityViolationException("unique violation");
            }
            saved.add(customer);
            byId.put(customer.getCustomerId(), customer);
            if (customer.getExternalCustomerKey() != null) {
                externalKeys.add(customer.getExternalCustomerKey());
            }
            if (customer.getPrimaryEmail() != null) {
                primaryEmails.add(customer.getPrimaryEmail());
            }
            return customer;
        }

        @Override
        public Optional<CustomerEntity> findActiveById(String customerId) {
            CustomerEntity entity = byId.get(customerId);
            if (entity == null || entity.getDeletedAt() != null) {
                return Optional.empty();
            }
            return Optional.of(entity);
        }

        @Override
        public Optional<CustomerEntity> findLatestActiveByOwnerUserId(String ownerUserId) {
            return ownerLookup;
        }

        @Override
        public Optional<CustomerEntity> findLatestActiveByCreatorUserId(String creatorUserId) {
            return creatorLookup;
        }

        @Override
        public List<CustomerEntity> findActiveCustomers() {
            return activeCustomers;
        }

        @Override
        public boolean existsByExternalCustomerKey(String externalCustomerKey) {
            return externalKeys.contains(externalCustomerKey);
        }

        @Override
        public boolean existsByPrimaryEmail(String primaryEmail) {
            return primaryEmails.contains(primaryEmail);
        }
    }

    private static final class CapturingAccessPolicyService extends CustomerAccessPolicyService {
        private ApiErrorException createException;
        private ApiErrorException listException;
        private ApiErrorException readException;
        private ApiErrorException updateException;
        private ApiErrorException deleteException;
        private ApiErrorException ownershipException;

        @Override
        public void enforceCreateAccess(String role) {
            if (createException != null) {
                throw createException;
            }
        }

        @Override
        public void enforceListAccess(String role) {
            if (listException != null) {
                throw listException;
            }
        }

        @Override
        public void enforceReadAccess(String role) {
            if (readException != null) {
                throw readException;
            }
        }

        @Override
        public void enforceUpdateAccess(String role) {
            if (updateException != null) {
                throw updateException;
            }
        }

        @Override
        public void enforceDeleteAccess(String role) {
            if (deleteException != null) {
                throw deleteException;
            }
        }

        @Override
        public void enforceOwnershipIfRequired(
                String role,
                String actorUserId,
                String ownerUserId,
                String createdByUserId,
                String operation) {
            if (ownershipException != null) {
                throw ownershipException;
            }
        }
    }

    private static final class CapturingLifecycleAuditService extends CustomerLifecycleAuditService {
        private final List<SuccessAuditCall> successCalls = new ArrayList<>();
        private final List<FailureAuditCall> failureCalls = new ArrayList<>();

        private CapturingLifecycleAuditService() {
            super(null);
        }

        @Override
        public void recordSuccess(String customerId, String eventType, String actorUserId, String actorRole) {
            successCalls.add(new SuccessAuditCall(customerId, eventType, actorUserId, actorRole));
        }

        @Override
        public void recordFailure(
                String customerId,
                String eventType,
                String actorUserId,
                String actorRole,
                String reasonCode,
                String metadata) {
            failureCalls.add(new FailureAuditCall(customerId, eventType, actorUserId, actorRole, reasonCode, metadata));
        }
    }

    private static final class CapturingDeletionPolicyService extends CustomerDeletionPolicyService {
        private DeletionDecision nextDecision = new DeletionDecision(false, false, List.of(), "ALLOW_DELETE");
        private ApiErrorException enforceException;

        private CapturingDeletionPolicyService() {
            super(null, null);
        }

        @Override
        public DeletionDecision evaluateDeletion(CustomerEntity customer) {
            return nextDecision;
        }

        @Override
        public void enforceDeletionAllowed(DeletionDecision decision) {
            if (enforceException != null) {
                throw enforceException;
            }
        }
    }

    private static final class CapturingAuthService extends AuthService {
        private final List<RegisterCall> registerCalls = new ArrayList<>();
        private final List<EmailUpdateCall> updateEmailCalls = new ArrayList<>();
        private final List<String> deactivateIdentityCalls = new ArrayList<>();
        private UUID nextUserId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
        private IllegalStateException illegalState;
        private IllegalArgumentException illegalArgument;
        private IllegalStateException emailUpdateIllegalState;
        private IllegalArgumentException emailUpdateIllegalArgument;
        private boolean nextUpdateEmailResult = true;
        private boolean nextDeactivateIdentityResult = true;

        private CapturingAuthService() {
            super(null, null, null);
        }

        @Override
        public UUID register(String email, String password, String passwordConfirmation, String role) {
            registerCalls.add(new RegisterCall(email, password, passwordConfirmation, role));
            if (illegalState != null) {
                throw illegalState;
            }
            if (illegalArgument != null) {
                throw illegalArgument;
            }
            return nextUserId;
        }

        @Override
        public boolean updateIdentityEmail(String userId, String email) {
            updateEmailCalls.add(new EmailUpdateCall(userId, email));
            if (emailUpdateIllegalState != null) {
                throw emailUpdateIllegalState;
            }
            if (emailUpdateIllegalArgument != null) {
                throw emailUpdateIllegalArgument;
            }
            return nextUpdateEmailResult;
        }

        @Override
        public boolean deactivateIdentity(String userId) {
            deactivateIdentityCalls.add(userId);
            return nextDeactivateIdentityResult;
        }
    }
}
