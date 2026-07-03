package com.example.banking.api.statements.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.statements.StatementResponseMapper;
import com.example.banking.api.statements.schemas.StatementResponseSchema;
import com.example.banking.lib.errors.StatementErrors;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.models.statement.MonthlyStatementStatus;
import com.example.banking.models.statement.StatementGenerationMode;
import com.example.banking.services.AccountRepository;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.CustomerRepository;
import com.example.banking.services.TransactionRepository;
import com.example.banking.services.statement.StatementAuthorizationService;

class StatementRetrievalControllerTest {

    private FakeStatementAuthorizationService authorizationService;
    private FakeCustomerPrincipalResolver principalResolver;
    private FakeStatementResponseMapper responseMapper;
    private FakeTransactionRepository transactionRepository;
    private FakeAccountRepository accountRepository;
    private FakeCustomerRepository customerRepository;

    private StatementRetrievalController controller;

    @BeforeEach
    void setUp() {
        authorizationService = new FakeStatementAuthorizationService();
        principalResolver = new FakeCustomerPrincipalResolver();
        responseMapper = new FakeStatementResponseMapper();
        transactionRepository = new FakeTransactionRepository();
        accountRepository = new FakeAccountRepository();
        customerRepository = new FakeCustomerRepository();

        controller = new StatementRetrievalController(
                authorizationService,
                principalResolver,
                responseMapper,
                transactionRepository,
                accountRepository,
                customerRepository);
    }

    @Test
    void getByIdResolvesPrincipalAndMapsResponse() {
        MonthlyStatement statement = statement(
                "11111111-1111-1111-1111-111111111111",
                "2026-07",
                1,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"));
        authorizationService.statementToReturn = statement;

        StatementResponseSchema expected = new StatementResponseSchema(
                statement.getStatementId(),
                statement.getAccountId(),
                statement.getPeriodYearMonth(),
                1,
                "100.00",
                "110.00",
                "AUD",
                "GENERATED",
                statement.getArtifactUri(),
                statement.getGeneratedAtUtc().toString());
        responseMapper.nextResponse = expected;

        ResponseEntity<StatementResponseSchema> response =
                controller.getById(statement.getStatementId(), null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        assertEquals(statement.getStatementId(), authorizationService.lastStatementId);
        assertEquals("principal-user", authorizationService.lastActorUserId);
        assertEquals("CUSTOMER", authorizationService.lastRole);
        assertSame(statement, responseMapper.lastStatement);
    }

    @Test
    void downloadArtifactRejectsWhenArtifactVersionMissingOrMismatch() {
        MonthlyStatement missingVersion = statement(
                "22222222-2222-2222-2222-222222222222",
                "2026-07",
                null,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"));
        authorizationService.statementToReturn = missingVersion;

        ApiErrorException missingVersionError = assertThrows(ApiErrorException.class,
                () -> controller.downloadArtifact(missingVersion.getStatementId(), 1, null));
        assertEquals("STATEMENT_NOT_FOUND", missingVersionError.getCode());

        MonthlyStatement mismatchVersion = statement(
                "33333333-3333-3333-3333-333333333333",
                "2026-07",
                2,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"));
        authorizationService.statementToReturn = mismatchVersion;

        ApiErrorException mismatchError = assertThrows(ApiErrorException.class,
                () -> controller.downloadArtifact(mismatchVersion.getStatementId(), 1, null));
        assertEquals("STATEMENT_NOT_FOUND", mismatchError.getCode());
    }

    @Test
    void downloadArtifactRendersNoTransactionsWhenRepositoryReturnsNullAndNoAccount() {
        MonthlyStatement statement = statement(
                "44444444-4444-4444-4444-444444444444",
                "////",
                1,
                null,
                null);
        statement.setCurrencyCode(null);
        statement.setOpeningBalance(null);
        statement.setClosingBalance(null);
        statement.setGeneratedAtUtc(null);

        authorizationService.statementToReturn = statement;
        transactionRepository.transactionsForPeriod = null;
        accountRepository.account = Optional.empty();
        customerRepository.customer = Optional.empty();

        ResponseEntity<byte[]> response = controller.downloadArtifact(statement.getStatementId(), 1, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("statement-statement-v1.pdf"));

        String pdfText = new String(response.getBody(), StandardCharsets.US_ASCII);
        assertTrue(pdfText.startsWith("%PDF"));
        assertTrue(pdfText.contains("Account Name: N/A"));
        assertTrue(pdfText.contains("Customer Name: N/A"));
        assertTrue(pdfText.contains("Statement Period: ////"));
        assertTrue(pdfText.contains("Generated Date: N/A"));
        assertTrue(pdfText.contains("No transactions were posted in this statement period."));
    }

    @Test
    void downloadArtifactRendersEscapedTextTruncationAndTransactionTypes() {
        String longStatementId = "aaaaaaaa-aaaa-bbbb-cccc-1234567890abcdef";
        MonthlyStatement statement = statement(
                longStatementId,
                "2026-07",
                3,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"));

        authorizationService.statementToReturn = statement;

        AccountEntity account = new AccountEntity();
        account.setAccountId(statement.getAccountId());
        account.setCustomerId("cust-1");
        account.setNickname("Primary (Ops) \\\\ Vault");
        account.setAccountType("CHECKING");
        account.setCheckingNumber(1234);

        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId("cust-1");
        customer.setLegalName("Jane (ACME) \\\\ Co");

        accountRepository.account = Optional.of(account);
        customerRepository.customer = Optional.of(customer);
        transactionRepository.transactionsForPeriod = buildManyTransactions();

        ResponseEntity<byte[]> response = controller.downloadArtifact(statement.getStatementId(), 3, null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("statement-2026-07-v3.pdf"));

        String pdfText = new String(response.getBody(), StandardCharsets.US_ASCII);
        String expectedRef = "aaaaaaaa-aaa...90abcdef";

        assertTrue(pdfText.contains("Statement Ref: " + expectedRef));
        assertTrue(pdfText.contains("Customer Name: Jane \\(ACME\\) \\\\\\\\ Co"));
        assertTrue(pdfText.contains("Account Name: Primary \\(Ops\\) \\\\\\\\ Vault"));
        assertTrue(pdfText.contains("Statement Period: 2026/07/01-2026/07/31"));

        assertTrue(pdfText.contains("Deposit"));
        assertTrue(pdfText.contains("Withdrawal"));
        assertTrue(pdfText.contains("Transfer Debit"));
        assertTrue(pdfText.contains("Transfer Credit"));
        assertTrue(pdfText.contains("Transaction"));

        assertTrue(pdfText.contains("-5.00 AUD"));
        assertTrue(pdfText.contains("+7.00 AUD"));
        assertTrue(pdfText.contains("Showing first 16 of 18 transactions in this period."));
    }

    @Test
    void downloadArtifactRendersAccountTypeFallbackAndNoOverflowNoticeForSmallTransactionList() {
        MonthlyStatement statement = statement(
                "55555555-5555-5555-5555-555555555555",
                "2026-07",
                1,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"));
        authorizationService.statementToReturn = statement;

        AccountEntity account = new AccountEntity();
        account.setAccountId(statement.getAccountId());
        account.setCustomerId("cust-2");
        account.setNickname(null);
        account.setAccountType("CHECKING");
        account.setCheckingNumber(null);
        accountRepository.account = Optional.of(account);

        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId("cust-2");
        customer.setLegalName("Casey Customer");
        customerRepository.customer = Optional.of(customer);

        transactionRepository.transactionsForPeriod = List.of(
                transaction(
                        "tx-small-1",
                        TransactionType.DEPOSIT,
                        "10.00",
                        "AUD",
                        "110.00",
                        Instant.parse("2026-07-10T10:00:00Z")));

        ResponseEntity<byte[]> response = controller.downloadArtifact(statement.getStatementId(), 1, null);

        assertEquals(200, response.getStatusCode().value());
        String pdfText = new String(response.getBody(), StandardCharsets.US_ASCII);
        assertTrue(pdfText.contains("Account Name: CHECKING Account"));
        assertTrue(!pdfText.contains("No transactions were posted in this statement period."));
        assertTrue(!pdfText.contains("Showing first "));
    }

    @Test
    void privateHelperBranchesAreCoveredViaReflection() {
        assertEquals("N/A", invokePrivate("safeText", new Class<?>[] { String.class }, (Object) null));
        assertEquals("N/A", invokePrivate("safeText", new Class<?>[] { String.class }, "   "));
        assertEquals("text", invokePrivate("safeText", new Class<?>[] { String.class }, " text "));

        assertEquals("0.00", invokePrivate("safeAmount", new Class<?>[] { BigDecimal.class }, (Object) null));
        assertEquals("12.35", invokePrivate("safeAmount", new Class<?>[] { BigDecimal.class }, new BigDecimal("12.345")));

        assertEquals(new BigDecimal("0.00"),
                invokePrivate("safeAmountValue", new Class<?>[] { BigDecimal.class }, (Object) null));
        assertEquals(new BigDecimal("7.89"),
                invokePrivate("safeAmountValue", new Class<?>[] { BigDecimal.class }, new BigDecimal("7.891")));

        assertEquals("12.35 AUD",
                invokePrivate("formatMoney", new Class<?>[] { BigDecimal.class, String.class }, new BigDecimal("12.345"), "AUD"));
        assertEquals("+3.33 AUD",
                invokePrivate("formatSignedMoney", new Class<?>[] { BigDecimal.class, String.class }, new BigDecimal("3.331"), "AUD"));
        assertEquals("-3.33 AUD",
                invokePrivate("formatSignedMoney", new Class<?>[] { BigDecimal.class, String.class }, new BigDecimal("-3.331"), "AUD"));

        assertEquals(new BigDecimal("0.00"),
                invokePrivate("calculateSignedAmount", new Class<?>[] { TransactionEntity.class }, (Object) null));

        TransactionEntity debitTxn = transaction("tx-debit", TransactionType.TRANSFER_DEBIT, "4.00", "AUD", "90.00", Instant.parse("2026-07-01T10:00:00Z"));
        assertEquals(new BigDecimal("-4.00"),
                invokePrivate("calculateSignedAmount", new Class<?>[] { TransactionEntity.class }, debitTxn));

        TransactionEntity withdrawalTxn = transaction("tx-withdrawal", TransactionType.WITHDRAWAL, "5.00", "AUD", "85.00", Instant.parse("2026-07-01T11:00:00Z"));
        assertEquals(new BigDecimal("-5.00"),
                invokePrivate("calculateSignedAmount", new Class<?>[] { TransactionEntity.class }, withdrawalTxn));

        TransactionEntity depositTxn = transaction("tx-deposit", TransactionType.DEPOSIT, "6.00", "AUD", "91.00", Instant.parse("2026-07-01T12:00:00Z"));
        assertEquals(new BigDecimal("6.00"),
                invokePrivate("calculateSignedAmount", new Class<?>[] { TransactionEntity.class }, depositTxn));

        assertEquals("N/A", invokePrivate("formatLocalDate", new Class<?>[] { Instant.class }, (Object) null));
        String localDate = invokePrivate("formatLocalDate", new Class<?>[] { Instant.class }, Instant.parse("2026-07-01T12:00:00Z"));
        assertTrue(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$").matcher(localDate).matches());

        assertEquals("N/A", invokePrivate("formatGeneratedDate", new Class<?>[] { Instant.class }, (Object) null));
        String generatedDate = invokePrivate("formatGeneratedDate", new Class<?>[] { Instant.class }, Instant.parse("2026-07-01T12:00:00Z"));
        assertTrue(Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$").matcher(generatedDate).matches());

        assertEquals("N/A", invokePrivate("resolveAccountName", new Class<?>[] { AccountEntity.class }, (Object) null));

        AccountEntity nicknameAccount = new AccountEntity();
        nicknameAccount.setNickname("  Payroll  ");
        assertEquals("Payroll",
                invokePrivate("resolveAccountName", new Class<?>[] { AccountEntity.class }, nicknameAccount));

        AccountEntity numberedAccount = new AccountEntity();
        numberedAccount.setNickname("   ");
        numberedAccount.setAccountType("CHECKING");
        numberedAccount.setCheckingNumber(77);
        assertEquals("CHECKING #77",
                invokePrivate("resolveAccountName", new Class<?>[] { AccountEntity.class }, numberedAccount));

        AccountEntity typedAccount = new AccountEntity();
        typedAccount.setNickname("   ");
        typedAccount.setAccountType("SAVINGS");
        typedAccount.setCheckingNumber(0);
        assertEquals("SAVINGS Account",
                invokePrivate("resolveAccountName", new Class<?>[] { AccountEntity.class }, typedAccount));

        assertEquals("Transaction", invokePrivate("toReadableTransactionType", new Class<?>[] { TransactionType.class }, (Object) null));
        assertEquals("Deposit", invokePrivate("toReadableTransactionType", new Class<?>[] { TransactionType.class }, TransactionType.DEPOSIT));

        assertEquals("N/A", invokePrivate("shortenTransactionId", new Class<?>[] { String.class }, (Object) null));
        assertEquals("short-id", invokePrivate("shortenTransactionId", new Class<?>[] { String.class }, "short-id"));
        assertEquals("12345678...", invokePrivate("shortenTransactionId", new Class<?>[] { String.class }, "1234567890123456"));

        assertEquals("N/A", invokePrivate("formatStatementReference", new Class<?>[] { String.class }, (Object) null));
        assertEquals("short-ref", invokePrivate("formatStatementReference", new Class<?>[] { String.class }, "short-ref"));
        assertEquals("123456789012...90abcdef",
                invokePrivate("formatStatementReference", new Class<?>[] { String.class }, "12345678901234567890abcdef"));

        MonthlyStatement nullStatement = null;
        assertEquals("N/A", invokePrivate("formatStatementPeriodRange", new Class<?>[] { MonthlyStatement.class }, nullStatement));

        MonthlyStatement invalidNoBounds = statement("ref-1", "bad-period", 1, null, null);
        assertEquals("bad-period",
                invokePrivate("formatStatementPeriodRange", new Class<?>[] { MonthlyStatement.class }, invalidNoBounds));

        MonthlyStatement invalidMissingEnd = statement(
            "ref-1b",
            "bad-period",
            1,
            Instant.parse("2026-07-01T00:00:00Z"),
            null);
        assertEquals("bad-period",
            invokePrivate("formatStatementPeriodRange", new Class<?>[] { MonthlyStatement.class }, invalidMissingEnd));

        MonthlyStatement invalidWithBounds = statement(
                "ref-2",
                "bad-period",
                1,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"));
        assertEquals("2026/07/01-2026/07/31",
                invokePrivate("formatStatementPeriodRange", new Class<?>[] { MonthlyStatement.class }, invalidWithBounds));

        MonthlyStatement invalidWithEqualBounds = statement(
                "ref-3",
                "bad-period",
                1,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"));
        assertEquals("2026/07/01-2026/07/01",
                invokePrivate("formatStatementPeriodRange", new Class<?>[] { MonthlyStatement.class }, invalidWithEqualBounds));

        MonthlyStatement parseable = statement("ref-4", "2026-07", 1, null, null);
        assertEquals("2026/07/01-2026/07/31",
                invokePrivate("formatStatementPeriodRange", new Class<?>[] { MonthlyStatement.class }, parseable));

        MonthlyStatement filenameNormal = statement("ref-5", "2026-07", 2, null, null);
        assertEquals("statement-2026-07-v2.pdf",
                invokePrivate("buildArtifactFileName", new Class<?>[] { MonthlyStatement.class, int.class }, filenameNormal, 2));

        MonthlyStatement filenameFallback = statement("ref-6", "////", 2, null, null);
        assertEquals("statement-statement-v2.pdf",
                invokePrivate("buildArtifactFileName", new Class<?>[] { MonthlyStatement.class, int.class }, filenameFallback, 2));

        assertEquals("a\\\\b\\(c\\)",
                invokePrivate("escapePdfText", new Class<?>[] { String.class }, "a\\b(c)"));
    }

    private List<TransactionEntity> buildManyTransactions() {
        List<TransactionEntity> transactions = new ArrayList<>();

        transactions.add(transaction(
                "1234567890abcdef-1",
                TransactionType.DEPOSIT,
                "7.00",
                "AUD",
                "107.00",
                Instant.parse("2026-07-01T12:00:00Z")));
        transactions.add(transaction(
                "1234567890abcdef-2",
                TransactionType.WITHDRAWAL,
                "5.00",
                "AUD",
                "102.00",
                Instant.parse("2026-07-02T12:00:00Z")));
        transactions.add(transaction(
                "1234567890abcdef-3",
                TransactionType.TRANSFER_DEBIT,
                "4.00",
                "AUD",
                "98.00",
                Instant.parse("2026-07-03T12:00:00Z")));
        transactions.add(transaction(
                "1234567890abcdef-4",
                TransactionType.TRANSFER_CREDIT,
                "6.00",
                "AUD",
                "104.00",
                Instant.parse("2026-07-04T12:00:00Z")));

        TransactionEntity nullType = transaction(
                "short-id",
                null,
                "3.00",
                "AUD",
                "107.00",
                null);
        transactions.add(nullType);

        TransactionEntity nullMoney = transaction(
                null,
                TransactionType.DEPOSIT,
                null,
                "   ",
                null,
                Instant.parse("2026-07-05T12:00:00Z"));
        transactions.add(nullMoney);

        for (int i = 0; i < 12; i++) {
            transactions.add(transaction(
                    "1234567890abcdef-extra-" + i,
                    TransactionType.DEPOSIT,
                    "1.00",
                    "AUD",
                    "108.00",
                    Instant.parse("2026-07-06T12:00:00Z")));
        }

        return transactions;
    }

    private static TransactionEntity transaction(
            String transactionId,
            TransactionType transactionType,
            String amount,
            String currency,
            String balanceAfter,
            Instant postedAtUtc) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount == null ? null : new BigDecimal(amount));
        transaction.setCurrencyCode(currency);
        transaction.setBalanceAfter(balanceAfter == null ? null : new BigDecimal(balanceAfter));
        transaction.setPostedAtUtc(postedAtUtc);
        return transaction;
    }

    private static MonthlyStatement statement(
            String statementId,
            String periodYearMonth,
            Integer artifactVersion,
            Instant periodStartUtc,
            Instant periodEndUtc) {
        MonthlyStatement statement = new MonthlyStatement();
        statement.setStatementId(statementId);
        statement.setAccountId("acc-1");
        statement.setPeriodYearMonth(periodYearMonth);
        statement.setPeriodStartUtc(periodStartUtc);
        statement.setPeriodEndUtc(periodEndUtc);
        statement.setOpeningBalance(new BigDecimal("100.00"));
        statement.setClosingBalance(new BigDecimal("110.00"));
        statement.setCurrencyCode("AUD");
        statement.setArtifactVersion(artifactVersion);
        statement.setArtifactUri("s3://statements/" + statementId);
        statement.setGenerationMode(StatementGenerationMode.STANDARD);
        statement.setGeneratedAtUtc(Instant.parse("2026-07-15T12:00:00Z"));
        statement.setStatus(MonthlyStatementStatus.GENERATED);
        return statement;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = StatementRetrievalController.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(controller, args);
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (target instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FakeStatementAuthorizationService extends StatementAuthorizationService {
        MonthlyStatement statementToReturn;

        String lastStatementId;
        String lastActorUserId;
        String lastRole;

        FakeStatementAuthorizationService() {
            super(null, null, null, null);
        }

        @Override
        public MonthlyStatement readStatementById(String statementId, String actorUserId, String role) {
            lastStatementId = statementId;
            lastActorUserId = actorUserId;
            lastRole = role;
            return statementToReturn;
        }
    }

    private static final class FakeCustomerPrincipalResolver extends CustomerPrincipalResolver {
        CustomerPrincipal nextPrincipal = new CustomerPrincipal("principal-user", "CUSTOMER");
        Authentication lastAuthentication;

        @Override
        public CustomerPrincipal resolve(Authentication authentication) {
            lastAuthentication = authentication;
            return nextPrincipal;
        }
    }

    private static final class FakeStatementResponseMapper extends StatementResponseMapper {
        MonthlyStatement lastStatement;
        StatementResponseSchema nextResponse;

        @Override
        public StatementResponseSchema toResponse(MonthlyStatement statement) {
            lastStatement = statement;
            if (nextResponse != null) {
                return nextResponse;
            }
            return super.toResponse(statement);
        }
    }

    private static final class FakeTransactionRepository implements TransactionRepository {
        List<TransactionEntity> transactionsForPeriod = List.of();

        @Override
        public TransactionEntity save(TransactionEntity transaction) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public List<TransactionEntity> saveAll(List<TransactionEntity> transactions) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public Optional<TransactionEntity> findById(String transactionId) {
            return Optional.empty();
        }

        @Override
        public List<TransactionEntity> findAccountTransactionsForPeriod(String accountId, Instant periodStartUtc, Instant periodEndUtc) {
            return transactionsForPeriod;
        }

        @Override
        public List<TransactionEntity> findCustomerTransactionsForPeriod(String customerId, Instant periodStartUtc, Instant periodEndUtc) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public org.springframework.data.domain.Page<TransactionEntity> findAccountHistory(
                String accountId,
                Instant startDateUtc,
                Instant endDateUtc,
                TransactionType transactionType,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public org.springframework.data.domain.Page<TransactionEntity> findCustomerHistory(
                String customerId,
                Instant startDateUtc,
                Instant endDateUtc,
                TransactionType transactionType,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not used");
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        Optional<AccountEntity> account = Optional.empty();

        @Override
        public AccountEntity save(AccountEntity account) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public Optional<AccountEntity> findActiveById(String accountId) {
            return account;
        }

        @Override
        public List<AccountEntity> findActiveByCustomerId(String customerId) {
            return List.of();
        }

        @Override
        public boolean existsByCustomerId(String customerId) {
            return false;
        }

        @Override
        public boolean existsByAccountNumber(String accountNumber) {
            return false;
        }

        @Override
        public boolean existsByCustomerIdAndCheckingNumber(String customerId, int checkingNumber) {
            return false;
        }

        @Override
        public int nextCheckingNumber(String customerId) {
            return 1;
        }
    }

    private static final class FakeCustomerRepository implements CustomerRepository {
        Optional<CustomerEntity> customer = Optional.empty();

        @Override
        public CustomerEntity save(CustomerEntity customer) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public Optional<CustomerEntity> findActiveById(String customerId) {
            return customer;
        }

        @Override
        public Optional<CustomerEntity> findLatestActiveByOwnerUserId(String ownerUserId) {
            return Optional.empty();
        }

        @Override
        public Optional<CustomerEntity> findLatestActiveByCreatorUserId(String creatorUserId) {
            return Optional.empty();
        }

        @Override
        public List<CustomerEntity> findActiveCustomers() {
            return List.of();
        }

        @Override
        public boolean existsByExternalCustomerKey(String externalCustomerKey) {
            return false;
        }

        @Override
        public boolean existsByPrimaryEmail(String primaryEmail) {
            return false;
        }
    }
}
