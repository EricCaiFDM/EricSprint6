package com.example.banking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.banking.api.account.dto.DeleteAccountRequest;
import com.example.banking.api.account.dto.GetAccountRequest;
import com.example.banking.api.account.dto.ListAccountsRequest;
import com.example.banking.models.AccountDeletionPolicyCheckEntity;
import com.example.banking.models.AccountEligibilityCheckEntity;
import com.example.banking.models.AccountLifecycleEventEntity;
import com.example.banking.models.AuthEventEntity;
import com.example.banking.models.CustomerLifecycleEventEntity;
import com.example.banking.models.DeletionPolicyCheckEntity;
import com.example.banking.models.NotificationDeliveryOutcomeEntity;
import com.example.banking.models.StandingOrder;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleEventEntity;
import com.example.banking.models.StandingOrderLifecycleState;
import com.example.banking.models.Transaction;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionLifecycleEventEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.models.TransferLinkEntity;
import com.example.banking.models.insights.InsightCategorySummary;
import com.example.banking.models.insights.InsightConfidenceMetadata;
import com.example.banking.models.insights.InsightRetrievalEvent;
import com.example.banking.models.statement.StatementAccessPolicy;
import com.example.banking.models.statement.StatementActivitySummary;
import com.example.banking.models.statement.StatementGenerationEvent;
import com.example.banking.models.statement.StatementRetrievalEvent;

import jakarta.persistence.Id;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LowCoverageModelAndDtoTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Object UNSUPPORTED = new Object();

    @Test
    void accountRequestRecordsValidateUuidShape() {
        String uuid = UUID.randomUUID().toString();
        GetAccountRequest getValid = new GetAccountRequest(uuid);
        DeleteAccountRequest deleteValid = new DeleteAccountRequest(uuid);

        assertEquals(uuid, getValid.accountId());
        assertEquals(uuid, deleteValid.accountId());
        assertTrue(VALIDATOR.validate(getValid).isEmpty());
        assertTrue(VALIDATOR.validate(deleteValid).isEmpty());

        assertFalse(VALIDATOR.validate(new GetAccountRequest("")).isEmpty());
        assertFalse(VALIDATOR.validate(new DeleteAccountRequest("not-a-uuid")).isEmpty());
    }

    @Test
    void listAccountsRequestStoresValues() {
        ListAccountsRequest request = new ListAccountsRequest("cust-1", 2, 25, "CHECKING", "ACTIVE");

        assertEquals("cust-1", request.customerId());
        assertEquals(2, request.page());
        assertEquals(25, request.pageSize());
        assertEquals("CHECKING", request.accountType());
        assertEquals("ACTIVE", request.status());
    }

    @Test
    void recordMappersConvertEntitiesToApiRecords() {
        StandingOrderEntity standingOrderEntity = new StandingOrderEntity();
        standingOrderEntity.setStandingOrderId("so-1");
        standingOrderEntity.setSourceAccountId("acc-1");
        standingOrderEntity.setDestinationAccountId("acc-2");
        standingOrderEntity.setAmount(new BigDecimal("10.50"));
        standingOrderEntity.setCurrencyCode("USD");
        standingOrderEntity.setCadence(StandingOrderCadence.WEEKLY);
        standingOrderEntity.setLifecycleState(StandingOrderLifecycleState.ACTIVE);
        standingOrderEntity.setNextExecutionAtUtc(Instant.parse("2026-07-01T00:00:00Z"));
        standingOrderEntity.setEffectiveFromUtc(Instant.parse("2026-06-01T00:00:00Z"));
        standingOrderEntity.setEffectiveToUtc(null);
        standingOrderEntity.setRetryPolicyCode("STANDARD");

        StandingOrder standingOrder = StandingOrder.fromEntity(standingOrderEntity);
        assertEquals("so-1", standingOrder.standingOrderId());
        assertEquals("10.50", standingOrder.amount());
        assertEquals(StandingOrderCadence.WEEKLY, standingOrder.cadence());

        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setTransactionId("txn-1");
        transactionEntity.setAccountId("acc-1");
        transactionEntity.setTransactionType(TransactionType.DEPOSIT);
        transactionEntity.setAmount(new BigDecimal("50.00"));
        transactionEntity.setCurrencyCode("USD");
        transactionEntity.setPostedAtUtc(Instant.parse("2026-06-29T10:00:00Z"));
        transactionEntity.setCorrelationId("corr-1");
        transactionEntity.setBalanceAfter(new BigDecimal("150.00"));

        Transaction transaction = Transaction.fromEntity(transactionEntity);
        assertEquals("txn-1", transaction.transactionId());
        assertEquals("50.00", transaction.amount());
        assertEquals("150.00", transaction.balanceAfter());
    }

    @Test
    void statementAccessPolicyRoleHelpersWork() {
        StatementAccessPolicy admin = new StatementAccessPolicy("u1", "ADMIN", "c1");
        StatementAccessPolicy customer = new StatementAccessPolicy("u2", "CUSTOMER", "c2");

        assertTrue(admin.isAdmin());
        assertFalse(admin.isCustomer());
        assertFalse(customer.isAdmin());
        assertTrue(customer.isCustomer());
    }

    @Test
    void healthControllerReturnsOk() {
        BankingApplication.HealthController controller = new BankingApplication.HealthController();

        assertEquals("OK", controller.health());
    }

    @Test
    void entityBeansRoundTripPropertiesAndLifecycleHooks() throws Exception {
        List<Object> withLifecycleHook = List.of(
                new StandingOrderEntity(),
                new TransactionEntity(),
                new NotificationDeliveryOutcomeEntity(),
                new TransferLinkEntity(),
                new StandingOrderLifecycleEventEntity(),
                new TransactionLifecycleEventEntity(),
                new InsightRetrievalEvent(),
                new InsightConfidenceMetadata(),
                new StatementActivitySummary(),
                new InsightCategorySummary(),
                new StatementGenerationEvent(),
                new StatementRetrievalEvent());

        for (Object bean : withLifecycleHook) {
            nullOutGeneratedFields(bean);
            invokeOnCreate(bean);
            assertIdGenerated(bean);
            assertReadWriteProperties(bean);
        }

        List<Object> constructorSeededIdBeans = List.of(
                new AccountEligibilityCheckEntity(),
                new AccountDeletionPolicyCheckEntity(),
                new DeletionPolicyCheckEntity(),
                new AccountLifecycleEventEntity(),
                new CustomerLifecycleEventEntity());

        for (Object bean : constructorSeededIdBeans) {
            assertIdGenerated(bean);
            assertReadWriteProperties(bean);
        }
    }

    @Test
    void authEventEntityExposesConstructorStateAndPrePersistTimestamp() throws Exception {
        AuthEventEntity event = new AuthEventEntity("evt-1", "LOGIN", "user@example.com", "SUCCESS", "NONE");

        assertEquals("evt-1", event.getId());
        assertEquals("LOGIN", event.getEventType());
        assertEquals("user@example.com", event.getIdentity());
        assertEquals("SUCCESS", event.getOutcome());
        assertEquals("NONE", event.getReasonCode());

        invokeOnCreate(event);
        assertNotNull(event.getCreatedAt());
    }

    private void assertReadWriteProperties(Object bean) throws Exception {
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors()) {
            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod == null || writeMethod == null || "class".equals(descriptor.getName())) {
                continue;
            }

            Object sample = sampleValue(descriptor.getPropertyType(), descriptor.getName());
            if (sample == UNSUPPORTED) {
                continue;
            }

            writeMethod.invoke(bean, sample);
            Object roundTrip = readMethod.invoke(bean);
            assertEquals(sample, roundTrip);
        }
    }

    private Object sampleValue(Class<?> type, String propertyName) {
        if (String.class.equals(type)) {
            return "value-" + propertyName;
        }
        if (int.class.equals(type) || Integer.class.equals(type)) {
            return 7;
        }
        if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return true;
        }
        if (BigDecimal.class.equals(type)) {
            return new BigDecimal("12.34");
        }
        if (Instant.class.equals(type)) {
            return Instant.parse("2026-06-29T00:00:00Z");
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        return UNSUPPORTED;
    }

    private void assertIdGenerated(Object bean) throws Exception {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Id.class)) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(bean);
            assertNotNull(value);
        }
    }

    private void nullOutGeneratedFields(Object bean) throws Exception {
        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (String.class.equals(field.getType())
                    && field.getName().toLowerCase(Locale.ROOT).contains("id")) {
                field.set(bean, null);
            }
            if (Instant.class.equals(field.getType())) {
                field.set(bean, null);
            }
        }
    }

    private void invokeOnCreate(Object bean) throws Exception {
        Method onCreate = bean.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(bean);
    }
}
