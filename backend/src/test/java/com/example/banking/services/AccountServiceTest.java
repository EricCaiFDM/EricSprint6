package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import org.springframework.http.HttpStatus;

import com.example.banking.api.account.dto.AccountListResponse;
import com.example.banking.api.account.dto.AccountResponse;
import com.example.banking.api.account.dto.CreateAccountRequest;
import com.example.banking.api.account.dto.UpdateAccountRequest;
import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.config.AccountModuleConfig;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;

class AccountServiceTest {

    private InMemoryAccountRepository accountRepository;
    private AccountResponsePolicyService responsePolicyService;
    private CapturingAccountAccessPolicyService accessPolicyService;
    private CapturingAccountLifecycleAuditService lifecycleAuditService;
    private CapturingAccountEligibilityService eligibilityService;
    private CapturingAccountDeletionPolicyService deletionPolicyService;
    private InMemoryCustomerJpaRepository customerJpaRepository;
    private AccountService service;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();

        AccountModuleConfig accountModuleConfig = new AccountModuleConfig();
        accountModuleConfig.setMaskingEnabled(true);
        responsePolicyService = new AccountResponsePolicyService(new AccountFieldMaskingService(accountModuleConfig));

        accessPolicyService = new CapturingAccountAccessPolicyService();
        lifecycleAuditService = new CapturingAccountLifecycleAuditService();
        eligibilityService = new CapturingAccountEligibilityService();
        deletionPolicyService = new CapturingAccountDeletionPolicyService();
        customerJpaRepository = new InMemoryCustomerJpaRepository();

        service = new AccountService(
                accountRepository,
                responsePolicyService,
                accessPolicyService,
                lifecycleAuditService,
                eligibilityService,
                deletionPolicyService,
                customerJpaRepository);
    }

    @Test
    void createAccountCheckingSuccessUsesOwnerFromCustomerAndCheckingSequence() {
        String customerId = UUID.randomUUID().toString();
        customerJpaRepository.customers.put(customerId, customer(customerId, "owner-user-1", "creator-1"));
        accountRepository.nextCheckingNumber = 1001;
        accountRepository.existingCheckingNumbers.add(1001);

        AccountResponse response = service.createAccount(
                new CreateAccountRequest(customerId, " checking ", "usd", " Primary ", new BigDecimal("1.23")),
                "actor-1",
                "CUSTOMER");

        assertEquals(customerId, response.customerId());
        assertEquals("CHECKING", response.accountType());
        assertEquals("0.0000", response.interestRate());
        assertEquals(1002, response.checkingNumber());
        assertEquals("ACTIVE", response.status());
        assertEquals("USD", response.currencyCode());
        assertEquals("Primary", response.nickname());
        assertEquals("0.00", response.balance());
        assertEquals(1, eligibilityService.calls.size());

        AccountEntity saved = accountRepository.saved.get(0);
        assertEquals("owner-user-1", saved.getOwnerUserId());
        assertEquals("actor-1", saved.getCreatedByUserId());
    }

    @Test
    void createAccountSavingsInterestRateBranchesAndOwnerFallback() {
        String customerId = UUID.randomUUID().toString();

        AccountResponse defaultRate = service.createAccount(
                new CreateAccountRequest(customerId, "SAVINGS", "USD", null, null),
                "actor-2",
                "ADMIN");
        assertEquals("0.0000", defaultRate.interestRate());
        assertEquals("actor-2", accountRepository.saved.get(0).getOwnerUserId());
        assertNull(accountRepository.saved.get(0).getCheckingNumber());

        AccountResponse roundedRate = service.createAccount(
                new CreateAccountRequest(customerId, "SAVINGS", "USD", null, new BigDecimal("1.23456")),
                "actor-2",
                "ADMIN");
        assertEquals("1.2346", roundedRate.interestRate());

        ApiErrorException negativeRate = captureCreateAccountError(
            new CreateAccountRequest(customerId, "SAVINGS", "USD", null, new BigDecimal("-0.01")),
            "actor-2",
            "ADMIN");
        assertEquals("ACCOUNT_VALIDATION_ERROR", negativeRate.getCode());
        assertEquals("interestRate", negativeRate.getField());
    }

    @Test
    void createAccountValidatesInputsAndRecordsFailures() {
        ApiErrorException nullCustomerId = captureCreateAccountError(
            new CreateAccountRequest(null, "CHECKING", "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", nullCustomerId.getCode());
        assertEquals("customerId", nullCustomerId.getField());

        ApiErrorException missingCustomerId = captureCreateAccountError(
            new CreateAccountRequest(" ", "CHECKING", "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", missingCustomerId.getCode());
        assertEquals("customerId", missingCustomerId.getField());

        ApiErrorException invalidCustomerId = captureCreateAccountError(
            new CreateAccountRequest("not-a-uuid", "CHECKING", "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", invalidCustomerId.getCode());
        assertEquals("customerId", invalidCustomerId.getField());

        ApiErrorException missingType = captureCreateAccountError(
            new CreateAccountRequest(UUID.randomUUID().toString(), " ", "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", missingType.getCode());
        assertEquals("accountType", missingType.getField());

        ApiErrorException nullType = captureCreateAccountError(
            new CreateAccountRequest(UUID.randomUUID().toString(), null, "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", nullType.getCode());
        assertEquals("accountType", nullType.getField());

        assertTrue(lifecycleAuditService.failureCalls.stream().anyMatch(call -> "CREATE".equals(call.eventType())));
    }

    @Test
    void createAccountHandlesAccessEligibilityAndScopeFailures() {
        String customerId = UUID.randomUUID().toString();

        accessPolicyService.createException = forbidden("create");
        ApiErrorException createForbidden = captureCreateAccountError(
            new CreateAccountRequest(customerId, "CHECKING", "USD", null, null),
            null,
            "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", createForbidden.getCode());
        accessPolicyService.createException = null;

        eligibilityService.exception = new ApiErrorException(HttpStatus.CONFLICT, "ACCOUNT_CONFLICT", "eligibility", "accountType");
        ApiErrorException eligibility = captureCreateAccountError(
            new CreateAccountRequest(customerId, "CHECKING", "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_CONFLICT", eligibility.getCode());
        eligibilityService.exception = null;

        accessPolicyService.listScopeException = forbidden("list");
        ApiErrorException scope = captureCreateAccountError(
            new CreateAccountRequest(customerId, "CHECKING", "USD", null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", scope.getCode());
    }

    @Test
    void getAccountByIdSupportsMaskedAndUnmaskedResponses() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB123456789012", "CHECKING", "ACTIVE", new BigDecimal("50.00"), "owner-1");
        accountRepository.byId.put(accountId, account);

        AccountResponse customerView = service.getAccountById(accountId, "owner-1", "CUSTOMER");
        assertEquals("****9012", customerView.accountNumber());

        AccountResponse adminView = service.getAccountById(accountId, "admin", "ADMIN");
        assertEquals("NB123456789012", adminView.accountNumber());
    }

    @Test
    void getAccountByIdValidationAndAccessFailuresAreCovered() {
        ApiErrorException missing = captureGetAccountByIdError(" ", "actor", "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", missing.getCode());

        ApiErrorException invalid = captureGetAccountByIdError("bad-id", "actor", "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", invalid.getCode());

        String accountId = UUID.randomUUID().toString();
        accessPolicyService.readException = forbidden("read");
        ApiErrorException readForbidden = captureGetAccountByIdError(accountId, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", readForbidden.getCode());
        accessPolicyService.readException = null;

        ApiErrorException notFound = captureGetAccountByIdError(accountId, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_NOT_FOUND", notFound.getCode());

        accountRepository.byId.put(accountId, account(accountId, UUID.randomUUID().toString(), "NB1234", "CHECKING", "ACTIVE", new BigDecimal("0.00"), "owner-1"));
        accessPolicyService.ownershipException = forbidden("read");
        ApiErrorException ownershipForbidden = captureGetAccountByIdError(accountId, "other", "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", ownershipForbidden.getCode());
    }

    @Test
    void listAccountsFiltersSortsAndPaginates() {
        String customerId = UUID.randomUUID().toString();
        accountRepository.byCustomer.put(customerId, new ArrayList<>(List.of(
                account(UUID.randomUUID().toString(), customerId, "NB-A", "CHECKING", "ACTIVE", new BigDecimal("10.00"), "owner"),
                account(UUID.randomUUID().toString(), customerId, "NB-B", "SAVINGS", "SUSPENDED", new BigDecimal("20.00"), "owner"),
                account(UUID.randomUUID().toString(), customerId, "NB-C", "CHECKING", "ACTIVE", new BigDecimal("30.00"), "owner"))));
        accountRepository.byCustomer.get(customerId).get(0).setOpenedAtUtc(Instant.parse("2026-06-01T00:00:00Z"));
        accountRepository.byCustomer.get(customerId).get(1).setOpenedAtUtc(Instant.parse("2026-06-03T00:00:00Z"));
        accountRepository.byCustomer.get(customerId).get(2).setOpenedAtUtc(Instant.parse("2026-06-02T00:00:00Z"));

        AccountListResponse filtered = service.listAccounts(customerId, 1, 10, "checking", "active", "actor", "ADMIN");
        assertEquals(2, filtered.items().size());
        assertEquals("NB-C", filtered.items().get(0).accountNumber());
        assertEquals("NB-A", filtered.items().get(1).accountNumber());

        AccountListResponse defaultFilter = service.listAccounts(customerId, 0, 200, null, " ", null, "CUSTOMER");
        assertEquals(1, defaultFilter.page());
        assertEquals(100, defaultFilter.pageSize());
        assertEquals(3, defaultFilter.totalItems());
        assertEquals(1, defaultFilter.totalPages());
        assertEquals("actor", lifecycleAuditService.successCalls.stream().filter(call -> "LIST".equals(call.eventType())).findFirst().orElseThrow().actorUserId());
    }

    @Test
    void listAccountsValidationAndAccessFailuresAreCovered() {
        String customerId = UUID.randomUUID().toString();

        accessPolicyService.listException = forbidden("list");
        ApiErrorException listForbidden = captureListAccountsError(customerId, 1, 10, null, null, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", listForbidden.getCode());
        accessPolicyService.listException = null;

        ApiErrorException invalidCustomerId = captureListAccountsError("bad-id", 1, 10, null, null, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", invalidCustomerId.getCode());

        accessPolicyService.listScopeException = forbidden("list");
        ApiErrorException scopeForbidden = captureListAccountsError(customerId, 1, 10, null, null, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", scopeForbidden.getCode());
    }

    @Test
    void updateAccountCoversNoFieldAndNicknameBranches() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB-U1", "CHECKING", "ACTIVE", new BigDecimal("40.00"), "owner");
        accountRepository.byId.put(accountId, account);

        ApiErrorException noField = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest(null, null, null, null),
            "owner",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", noField.getCode());

        AccountResponse updated = service.updateAccount(
                accountId,
                new UpdateAccountRequest("  Main ", null, null, null),
                "owner",
                "CUSTOMER");
        assertEquals("Main", updated.nickname());

        AccountResponse blankNick = service.updateAccount(
                accountId,
                new UpdateAccountRequest("   ", null, null, null),
                "owner",
                "CUSTOMER");
        assertNull(blankNick.nickname());
    }

    @Test
    void updateAccountStatusTransitionBranchesAreCovered() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB-U2", "CHECKING", null, new BigDecimal("40.00"), "owner");
        accountRepository.byId.put(accountId, account);

        AccountResponse same = service.updateAccount(accountId, new UpdateAccountRequest(null, "ACTIVE", null, null), "owner", "CUSTOMER");
        assertEquals("ACTIVE", same.status());

        account.setStatus("ACTIVE");
        assertEquals("SUSPENDED", service.updateAccount(accountId, new UpdateAccountRequest(null, "SUSPENDED", null, null), "owner", "CUSTOMER").status());

        account.setStatus("SUSPENDED");
        assertEquals("ACTIVE", service.updateAccount(accountId, new UpdateAccountRequest(null, "ACTIVE", null, null), "owner", "CUSTOMER").status());

        account.setStatus("ACTIVE");
        AccountResponse closed = service.updateAccount(accountId, new UpdateAccountRequest(null, "CLOSED", null, null), "owner", "CUSTOMER");
        assertEquals("CLOSED", closed.status());
        assertNotNull(account.getClosedAtUtc());

        account.setStatus("SUSPENDED");
        assertEquals("CLOSED", service.updateAccount(accountId, new UpdateAccountRequest(null, "CLOSED", null, null), "owner", "CUSTOMER").status());

        account.setStatus("CLOSED");
        ApiErrorException invalid = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest(null, "ACTIVE", null, null),
            "owner",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", invalid.getCode());
    }

    @Test
    void updateAccountInterestRateAndBalanceAdminBranchesAreCovered() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity checking = account(accountId, UUID.randomUUID().toString(), "NB-U3", "CHECKING", "ACTIVE", new BigDecimal("40.00"), "owner");
        accountRepository.byId.put(accountId, checking);

        accessPolicyService.adminFinancialUpdateException = forbidden("update");
        ApiErrorException financialForbidden = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest(null, null, new BigDecimal("1.00"), null),
            "owner",
            "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", financialForbidden.getCode());
        accessPolicyService.adminFinancialUpdateException = null;

        ApiErrorException wrongType = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest(null, null, new BigDecimal("1.00"), null),
            "owner",
            "ADMIN");
        assertEquals("ACCOUNT_VALIDATION_ERROR", wrongType.getCode());
        assertEquals("interestRate", wrongType.getField());

        String savingsId = UUID.randomUUID().toString();
        AccountEntity savings = account(savingsId, UUID.randomUUID().toString(), "NB-U4", "SAVINGS", "ACTIVE", new BigDecimal("10.00"), "owner");
        savings.setInterestRate(new BigDecimal("0.1000"));
        accountRepository.byId.put(savingsId, savings);

        AccountResponse interestUpdated = service.updateAccount(
                savingsId,
                new UpdateAccountRequest(null, null, new BigDecimal("1.23456"), null),
                "owner",
                "ADMIN");
        assertEquals("1.2346", interestUpdated.interestRate());

        ApiErrorException negative = captureUpdateAccountError(
            savingsId,
            new UpdateAccountRequest(null, null, new BigDecimal("-0.01"), null),
            "owner",
            "ADMIN");
        assertEquals("ACCOUNT_VALIDATION_ERROR", negative.getCode());
        assertEquals("interestRate", negative.getField());

        AccountResponse balanceUpdated = service.updateAccount(
                savingsId,
                new UpdateAccountRequest(null, null, null, new BigDecimal("123.456")),
                "owner",
                "ADMIN");
        assertEquals("123.46", balanceUpdated.balance());
    }

    @Test
    void updateAccountValidationLookupAndOwnershipFailuresAreCovered() {
        ApiErrorException missing = captureUpdateAccountError(
            " ",
            new UpdateAccountRequest("A", null, null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", missing.getCode());

        ApiErrorException invalidUuid = captureUpdateAccountError(
            "bad-id",
            new UpdateAccountRequest("A", null, null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", invalidUuid.getCode());

        String accountId = UUID.randomUUID().toString();
        accessPolicyService.updateException = forbidden("update");
        ApiErrorException updateForbidden = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest("A", null, null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", updateForbidden.getCode());
        accessPolicyService.updateException = null;

        ApiErrorException notFound = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest("A", null, null, null),
            "actor",
            "CUSTOMER");
        assertEquals("ACCOUNT_NOT_FOUND", notFound.getCode());

        accountRepository.byId.put(accountId, account(accountId, UUID.randomUUID().toString(), "NB-U5", "CHECKING", "ACTIVE", new BigDecimal("0.00"), "owner"));
        accessPolicyService.ownershipException = forbidden("update");
        ApiErrorException ownershipForbidden = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest("A", null, null, null),
            "other",
            "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", ownershipForbidden.getCode());
    }

        @Test
        void updateAccountCoversUnsupportedStatusTransitionBranch() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB-U6", "CHECKING", "ACTIVE", new BigDecimal("0.00"), "owner");
        accountRepository.byId.put(accountId, account);

        ApiErrorException invalid = captureUpdateAccountError(
            accountId,
            new UpdateAccountRequest(null, "PAUSED", null, null),
            "owner",
            "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", invalid.getCode());
        assertEquals("status", invalid.getField());
        }

        @Test
        void createAccountCoversBlankActorAndBlankCustomerOwnerFallbackAndAccountNumberLoop() {
        String customerId = UUID.randomUUID().toString();
            customerJpaRepository.customers.put(customerId, customer(customerId, "   ", "creator"));
            accountRepository.forceAccountNumberCollisionCount = 1;

            String result = service.createAccount(
                new CreateAccountRequest(customerId, "CHECKING", "USD", null, null),
                "   ",
                "CUSTOMER").accountNumber();

            assertTrue(result.startsWith("NB"));
            assertEquals(14, result.length());
            assertTrue(accountRepository.existsByAccountNumberCalls >= 2);
        assertEquals("anonymous", accountRepository.saved.get(accountRepository.saved.size() - 1).getCreatedByUserId());
        }

        @Test
        void createAccountUsesActorFallbackWhenCustomerOwnerIsNull() {
            String customerId = UUID.randomUUID().toString();
            customerJpaRepository.customers.put(customerId, customer(customerId, null, "creator"));

            service.createAccount(
                    new CreateAccountRequest(customerId, "CHECKING", "USD", null, null),
                    "actor-null-owner",
                    "CUSTOMER");

            assertEquals("actor-null-owner", accountRepository.saved.get(accountRepository.saved.size() - 1).getOwnerUserId());
        }

        @Test
        void listAccountsCoversBlankAccountTypeAndStatusAndAnonymousActor() {
        String customerId = UUID.randomUUID().toString();
        accountRepository.byCustomer.put(customerId, new ArrayList<>(List.of(
            account(UUID.randomUUID().toString(), customerId, "NB-L1", "CHECKING", "ACTIVE", new BigDecimal("1.00"), "owner"))));

        AccountListResponse response = service.listAccounts(customerId, 1, 10, "   ", null, null, "ADMIN");

        assertEquals(1, response.items().size());
        assertEquals("anonymous", lifecycleAuditService.successCalls.stream()
            .filter(call -> "LIST".equals(call.eventType()))
            .reduce((first, second) -> second)
            .orElseThrow()
            .actorUserId());
        }

    @Test
    void inMemoryCustomerJpaRepositoryDefaultAndNoopMethodsAreCovered() {
        InMemoryCustomerJpaRepository repository = new InMemoryCustomerJpaRepository();

        assertTrue(repository.findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc("owner").isEmpty());
        assertTrue(repository.findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc("creator").isEmpty());
        assertTrue(repository.findByDeletedAtIsNullOrderByCreatedAtUtcDesc().isEmpty());
        assertFalse(repository.existsByExternalCustomerKeyIgnoreCaseAndDeletedAtIsNull("ext-1"));
        assertFalse(repository.existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull("a@b.com"));

        repository.flush();
        repository.deleteAllInBatch(List.of());
        repository.deleteAllByIdInBatch(List.of("missing"));
        repository.deleteAllInBatch();

        assertNull(repository.getOne("missing"));
        assertNull(repository.getById("missing"));
        assertNull(repository.getReferenceById("missing"));
    }

    @Test
    void inMemoryCustomerJpaRepositoryCrudAndLookupMethodsAreCovered() {
        InMemoryCustomerJpaRepository repository = new InMemoryCustomerJpaRepository();

        CustomerEntity first = customer(UUID.randomUUID().toString(), "owner-1", "creator-1");
        CustomerEntity second = customer(UUID.randomUUID().toString(), "owner-2", "creator-2");
        CustomerEntity third = customer(UUID.randomUUID().toString(), "owner-3", "creator-3");
        CustomerEntity fourth = customer(UUID.randomUUID().toString(), "owner-4", "creator-4");
        CustomerEntity fifth = customer(UUID.randomUUID().toString(), "owner-5", "creator-5");

        assertSame(first, repository.save(first));

        List<CustomerEntity> savedMany = repository.saveAll(List.of(second, third));
        assertEquals(2, savedMany.size());
        assertSame(second, savedMany.get(0));
        assertSame(third, savedMany.get(1));

        assertSame(fourth, repository.saveAndFlush(fourth));

        List<CustomerEntity> flushedMany = repository.saveAllAndFlush(List.of(fifth));
        assertEquals(1, flushedMany.size());
        assertSame(fifth, flushedMany.get(0));

        assertEquals(5, repository.count());
        assertTrue(repository.existsById(first.getCustomerId()));
        assertSame(first, repository.findById(first.getCustomerId()).orElseThrow());
        assertEquals(5, repository.findAll().size());

        List<CustomerEntity> selected = repository.findAllById(List.of(first.getCustomerId(), "missing", third.getCustomerId()));
        assertEquals(2, selected.size());
        assertTrue(selected.contains(first));
        assertTrue(selected.contains(third));

        assertSame(first, repository.getOne(first.getCustomerId()));
        assertSame(second, repository.getById(second.getCustomerId()));
        assertSame(third, repository.getReferenceById(third.getCustomerId()));

        assertSame(first, repository.findByCustomerIdAndDeletedAtIsNull(first.getCustomerId()).orElseThrow());
        third.setDeletedAt(Instant.now());
        assertTrue(repository.findByCustomerIdAndDeletedAtIsNull(third.getCustomerId()).isEmpty());

        repository.deleteById(first.getCustomerId());
        assertFalse(repository.existsById(first.getCustomerId()));

        repository.delete(second);
        assertFalse(repository.existsById(second.getCustomerId()));

        repository.deleteAllById(List.of(third.getCustomerId()));
        assertFalse(repository.existsById(third.getCustomerId()));

        repository.deleteAll(List.of(fourth));
        assertFalse(repository.existsById(fourth.getCustomerId()));

        assertEquals(1, repository.count());
        repository.deleteAll();
        assertEquals(0, repository.count());
    }

    @Test
    void deleteAccountAllowPathSetsClosedAndTimestamps() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB-D1", "CHECKING", "ACTIVE", new BigDecimal("0.00"), "owner");
        account.setClosedAtUtc(null);
        accountRepository.byId.put(accountId, account);
        deletionPolicyService.nextDecision = new AccountDeletionPolicyService.DeletionDecision(false, false, List.of(), "ALLOW_DELETE");

        String result = service.deleteAccount(accountId, "owner", "ADMIN");

        assertEquals("CLOSED", result);
        assertEquals("CLOSED", account.getStatus());
        assertNotNull(account.getDeletedAt());
        assertNotNull(account.getClosedAtUtc());
        assertTrue(lifecycleAuditService.successCalls.stream().anyMatch(call -> "DELETE_SUCCESS".equals(call.eventType())));
    }

    @Test
    void deleteAccountKeepsExistingClosedAtWhenAlreadySet() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB-D2", "CHECKING", "CLOSED", new BigDecimal("0.00"), "owner");
        Instant closedAt = Instant.parse("2026-06-01T00:00:00Z");
        account.setClosedAtUtc(closedAt);
        accountRepository.byId.put(accountId, account);
        deletionPolicyService.nextDecision = new AccountDeletionPolicyService.DeletionDecision(false, false, List.of(), "ALLOW_DELETE");

        service.deleteAccount(accountId, "owner", "ADMIN");

        assertEquals(closedAt, account.getClosedAtUtc());
    }

    @Test
    void deleteAccountBlockDeleteBranchesAreCovered() {
        String accountId = UUID.randomUUID().toString();
        AccountEntity account = account(accountId, UUID.randomUUID().toString(), "NB-D3", "CHECKING", "ACTIVE", new BigDecimal("5.00"), "owner");
        accountRepository.byId.put(accountId, account);

        deletionPolicyService.nextDecision = new AccountDeletionPolicyService.DeletionDecision(true, false, List.of("BALANCE_NOT_ZERO"), "BLOCK_DELETE");
        deletionPolicyService.enforceException = new ApiErrorException(HttpStatus.CONFLICT, "ACCOUNT_DELETE_BLOCKED", "blocked", null);

        ApiErrorException blocked = captureDeleteAccountError(accountId, "owner", "ADMIN");
        assertEquals("ACCOUNT_DELETE_BLOCKED", blocked.getCode());
        assertTrue(lifecycleAuditService.failureCalls.stream().anyMatch(call -> "DELETE_BLOCKED".equals(call.eventType())));
    }

    @Test
    void deleteAccountValidationAndAccessFailuresAreCovered() {
        ApiErrorException missing = captureDeleteAccountError(" ", "actor", "CUSTOMER");
        assertEquals("ACCOUNT_VALIDATION_ERROR", missing.getCode());

        String accountId = UUID.randomUUID().toString();
        accessPolicyService.deleteException = forbidden("delete");
        ApiErrorException deleteForbidden = captureDeleteAccountError(accountId, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", deleteForbidden.getCode());
        accessPolicyService.deleteException = null;

        ApiErrorException notFound = captureDeleteAccountError(accountId, "actor", "CUSTOMER");
        assertEquals("ACCOUNT_NOT_FOUND", notFound.getCode());

        accountRepository.byId.put(accountId, account(accountId, UUID.randomUUID().toString(), "NB-D4", "CHECKING", "ACTIVE", new BigDecimal("0.00"), "owner"));
        accessPolicyService.ownershipException = forbidden("delete");
        ApiErrorException ownership = captureDeleteAccountError(accountId, "other", "CUSTOMER");
        assertEquals("ACCOUNT_FORBIDDEN", ownership.getCode());
    }

    private AccountEntity account(
            String accountId,
            String customerId,
            String accountNumber,
            String accountType,
            String status,
            BigDecimal balance,
            String ownerUserId) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCustomerId(customerId);
        account.setAccountNumber(accountNumber);
        account.setAccountType(accountType);
        account.setInterestRate(new BigDecimal("0.0000"));
        account.setCheckingNumber("CHECKING".equals(accountType) ? 1001 : null);
        account.setStatus(status);
        account.setNickname("nickname");
        account.setBalance(balance.setScale(2));
        account.setCurrencyCode("USD");
        account.setOpenedAtUtc(Instant.parse("2026-06-01T00:00:00Z"));
        account.setClosedAtUtc(null);
        account.setCreatedByUserId("creator");
        account.setOwnerUserId(ownerUserId);
        account.setUpdatedAtUtc(Instant.parse("2026-06-01T00:00:00Z"));
        return account;
    }

    private ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_FORBIDDEN",
                "Insufficient privileges to " + operation + " account",
                null);
    }

    private ApiErrorException captureCreateAccountError(
            CreateAccountRequest request,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.createAccount(request, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureGetAccountByIdError(
            String accountId,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.getAccountById(accountId, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureListAccountsError(
            String customerId,
            int page,
            int pageSize,
            String accountType,
            String status,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.listAccounts(customerId, page, pageSize, accountType, status, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureUpdateAccountError(
            String accountId,
            UpdateAccountRequest request,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.updateAccount(accountId, request, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureDeleteAccountError(String accountId, String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.deleteAccount(accountId, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private record SuccessAuditCall(String accountId, String eventType, String actorUserId, String actorRole) {
    }

    private record FailureAuditCall(
            String accountId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
    }

    private static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, AccountEntity> byId = new HashMap<>();
        private final Map<String, List<AccountEntity>> byCustomer = new HashMap<>();
        private final List<AccountEntity> saved = new ArrayList<>();
        private final Set<String> existingAccountNumbers = new HashSet<>();
        private final Set<Integer> existingCheckingNumbers = new HashSet<>();
        private int nextCheckingNumber = 1001;
        private int forceAccountNumberCollisionCount;
        private int existsByAccountNumberCalls;

        @Override
        public AccountEntity save(AccountEntity account) {
            saved.add(account);
            byId.put(account.getAccountId(), account);
            byCustomer.computeIfAbsent(account.getCustomerId(), ignored -> new ArrayList<>());
            if (!byCustomer.get(account.getCustomerId()).contains(account)) {
                byCustomer.get(account.getCustomerId()).add(account);
            }
            if (account.getAccountNumber() != null) {
                existingAccountNumbers.add(account.getAccountNumber());
            }
            if (account.getCheckingNumber() != null) {
                existingCheckingNumbers.add(account.getCheckingNumber());
            }
            return account;
        }

        @Override
        public Optional<AccountEntity> findActiveById(String accountId) {
            AccountEntity account = byId.get(accountId);
            if (account == null || account.getDeletedAt() != null) {
                return Optional.empty();
            }
            return Optional.of(account);
        }

        @Override
        public List<AccountEntity> findActiveByCustomerId(String customerId) {
            return byCustomer.getOrDefault(customerId, List.of()).stream()
                    .filter(account -> account.getDeletedAt() == null)
                    .toList();
        }

        @Override
        public boolean existsByCustomerId(String customerId) {
            return byCustomer.containsKey(customerId);
        }

        @Override
        public boolean existsByAccountNumber(String accountNumber) {
            existsByAccountNumberCalls++;
            if (forceAccountNumberCollisionCount > 0) {
                forceAccountNumberCollisionCount -= 1;
                return true;
            }
            return existingAccountNumbers.contains(accountNumber);
        }

        @Override
        public boolean existsByCustomerIdAndCheckingNumber(String customerId, int checkingNumber) {
            return existingCheckingNumbers.contains(checkingNumber);
        }

        @Override
        public int nextCheckingNumber(String customerId) {
            return nextCheckingNumber;
        }
    }

    private static final class CapturingAccountAccessPolicyService extends AccountAccessPolicyService {
        private ApiErrorException createException;
        private ApiErrorException readException;
        private ApiErrorException listException;
        private ApiErrorException updateException;
        private ApiErrorException adminFinancialUpdateException;
        private ApiErrorException deleteException;
        private ApiErrorException ownershipException;
        private ApiErrorException listScopeException;

        private CapturingAccountAccessPolicyService() {
            super(null);
        }

        @Override
        public void enforceCreateAccess(String role) {
            if (createException != null) {
                throw createException;
            }
        }

        @Override
        public void enforceReadAccess(String role) {
            if (readException != null) {
                throw readException;
            }
        }

        @Override
        public void enforceListAccess(String role) {
            if (listException != null) {
                throw listException;
            }
        }

        @Override
        public void enforceUpdateAccess(String role) {
            if (updateException != null) {
                throw updateException;
            }
        }

        @Override
        public void enforceAdminFinancialUpdateAccess(String role) {
            if (adminFinancialUpdateException != null) {
                throw adminFinancialUpdateException;
            }
        }

        @Override
        public void enforceDeleteAccess(String role) {
            if (deleteException != null) {
                throw deleteException;
            }
        }

        @Override
        public void enforceOwnershipIfRequired(String role, String actorUserId, String ownerUserId, String operation) {
            if (ownershipException != null) {
                throw ownershipException;
            }
        }

        @Override
        public void enforceListScope(String role, String actorUserId, String requestedCustomerId) {
            if (listScopeException != null) {
                throw listScopeException;
            }
        }
    }

    private static final class CapturingAccountLifecycleAuditService extends AccountLifecycleAuditService {
        private final List<SuccessAuditCall> successCalls = new ArrayList<>();
        private final List<FailureAuditCall> failureCalls = new ArrayList<>();

        private CapturingAccountLifecycleAuditService() {
            super(null);
        }

        @Override
        public void recordSuccess(String accountId, String eventType, String actorUserId, String actorRole) {
            successCalls.add(new SuccessAuditCall(accountId, eventType, actorUserId, actorRole));
        }

        @Override
        public void recordFailure(
                String accountId,
                String eventType,
                String actorUserId,
                String actorRole,
                String reasonCode,
                String metadata) {
            failureCalls.add(new FailureAuditCall(accountId, eventType, actorUserId, actorRole, reasonCode, metadata));
        }
    }

    private static final class CapturingAccountEligibilityService extends AccountEligibilityService {
        private final List<String> calls = new ArrayList<>();
        private ApiErrorException exception;

        private CapturingAccountEligibilityService() {
            super(null, null);
        }

        @Override
        public void enforceEligibility(String customerId, String accountType) {
            calls.add(customerId + "|" + accountType);
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static final class CapturingAccountDeletionPolicyService extends AccountDeletionPolicyService {
        private DeletionDecision nextDecision = new DeletionDecision(false, false, List.of(), "ALLOW_DELETE");
        private ApiErrorException enforceException;

        private CapturingAccountDeletionPolicyService() {
            super(null);
        }

        @Override
        public DeletionDecision evaluateDeletion(AccountEntity account) {
            return nextDecision;
        }

        @Override
        public void enforceDeletionAllowed(DeletionDecision decision) {
            if (enforceException != null) {
                throw enforceException;
            }
        }
    }

    private static final class InMemoryCustomerJpaRepository implements CustomerJpaRepository {
        private final Map<String, CustomerEntity> customers = new HashMap<>();

        @Override
        public Optional<CustomerEntity> findByCustomerIdAndDeletedAtIsNull(String customerId) {
            CustomerEntity customer = customers.get(customerId);
            if (customer == null || customer.getDeletedAt() != null) {
                return Optional.empty();
            }
            return Optional.of(customer);
        }

        @Override
        public Optional<CustomerEntity> findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(String ownerUserId) {
            return Optional.empty();
        }

        @Override
        public Optional<CustomerEntity> findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(String createdByUserId) {
            return Optional.empty();
        }

        @Override
        public List<CustomerEntity> findByDeletedAtIsNullOrderByCreatedAtUtcDesc() {
            return List.of();
        }

        @Override
        public boolean existsByExternalCustomerKeyIgnoreCaseAndDeletedAtIsNull(String externalCustomerKey) {
            return false;
        }

        @Override
        public boolean existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull(String primaryEmail) {
            return false;
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends CustomerEntity> S saveAndFlush(S entity) {
            customers.put(entity.getCustomerId(), entity);
            return entity;
        }

        @Override
        public <S extends CustomerEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
            List<S> list = new ArrayList<>();
            for (S entity : entities) {
                list.add(saveAndFlush(entity));
            }
            return list;
        }

        @Override
        public void deleteAllInBatch(Iterable<CustomerEntity> entities) {
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<String> strings) {
        }

        @Override
        public void deleteAllInBatch() {
        }

        @Override
        public CustomerEntity getOne(String s) {
            return customers.get(s);
        }

        @Override
        public CustomerEntity getById(String s) {
            return customers.get(s);
        }

        @Override
        public CustomerEntity getReferenceById(String s) {
            return customers.get(s);
        }

        @Override
        public <S extends CustomerEntity> S save(S entity) {
            customers.put(entity.getCustomerId(), entity);
            return entity;
        }

        @Override
        public <S extends CustomerEntity> List<S> saveAll(Iterable<S> entities) {
            List<S> list = new ArrayList<>();
            for (S entity : entities) {
                list.add(save(entity));
            }
            return list;
        }

        @Override
        public Optional<CustomerEntity> findById(String s) {
            return Optional.ofNullable(customers.get(s));
        }

        @Override
        public boolean existsById(String s) {
            return customers.containsKey(s);
        }

        @Override
        public List<CustomerEntity> findAll() {
            return new ArrayList<>(customers.values());
        }

        @Override
        public List<CustomerEntity> findAllById(Iterable<String> strings) {
            List<CustomerEntity> list = new ArrayList<>();
            for (String id : strings) {
                CustomerEntity customer = customers.get(id);
                if (customer != null) {
                    list.add(customer);
                }
            }
            return list;
        }

        @Override
        public long count() {
            return customers.size();
        }

        @Override
        public void deleteById(String s) {
            customers.remove(s);
        }

        @Override
        public void delete(CustomerEntity entity) {
            customers.remove(entity.getCustomerId());
        }

        @Override
        public void deleteAllById(Iterable<? extends String> strings) {
            for (String id : strings) {
                customers.remove(id);
            }
        }

        @Override
        public void deleteAll(Iterable<? extends CustomerEntity> entities) {
            for (CustomerEntity entity : entities) {
                customers.remove(entity.getCustomerId());
            }
        }

        @Override
        public void deleteAll() {
            customers.clear();
        }
    }

    private CustomerEntity customer(String customerId, String ownerUserId, String createdByUserId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId(customerId);
        customer.setOwnerUserId(ownerUserId);
        customer.setCreatedByUserId(createdByUserId);
        customer.setCreatedAtUtc(Instant.parse("2026-06-01T00:00:00Z"));
        customer.setUpdatedAtUtc(Instant.parse("2026-06-01T00:00:00Z"));
        return customer;
    }
}
