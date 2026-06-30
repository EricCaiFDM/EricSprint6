package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationRecipientScopeType;

class NotificationAccessPolicyTest {

    private final Map<String, AccountEntity> accounts = new ConcurrentHashMap<>();
    private final Map<String, CustomerEntity> customers = new ConcurrentHashMap<>();

    private NotificationAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new NotificationAccessPolicy(accountRepository(), customerRepository());
    }

    @Test
    void enforceTriggerAccessRejectsNullRoleAndAcceptsNormalizedRoles() {
        policy.enforceTriggerAccess(" admin ");
        policy.enforceTriggerAccess("CUSTOMER");

        ApiErrorException forbidden = assertThrows(
                ApiErrorException.class,
                () -> policy.enforceTriggerAccess(null));
        assertEquals("NOTIFICATION_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void requireRecipientScopeValidatesRequiredScopeInputs() {
        ApiErrorException missingScopeType = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        null,
                        "scope-1",
                        "ADMIN",
                        "actor-1",
                        "read"));
        assertEquals("NOTIFICATION_VALIDATION_ERROR", missingScopeType.getCode());
        assertEquals("recipientScopeId", missingScopeType.getField());

        ApiErrorException nullScopeId = assertThrows(
            ApiErrorException.class,
            () -> policy.requireRecipientScope(
                NotificationRecipientScopeType.CUSTOMER,
                null,
                "ADMIN",
                "actor-1",
                "read"));
        assertEquals("NOTIFICATION_VALIDATION_ERROR", nullScopeId.getCode());
        assertEquals("recipientScopeId", nullScopeId.getField());

        ApiErrorException blankScopeId = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        NotificationRecipientScopeType.CUSTOMER,
                        "   ",
                        "ADMIN",
                        "actor-1",
                        "read"));
        assertEquals("NOTIFICATION_VALIDATION_ERROR", blankScopeId.getCode());
        assertEquals("recipientScopeId", blankScopeId.getField());
    }

    @Test
        void requireRecipientScopeCoversEnsureScopeExistsBranchesForAdmin() {
        customers.put("cust-admin", customer("cust-admin", "owner-admin", "creator-admin"));
        accounts.put("acc-admin", account("acc-admin", "cust-admin", "owner-admin", "creator-admin"));

        policy.requireRecipientScope(
            NotificationRecipientScopeType.CUSTOMER,
            "cust-admin",
            "ADMIN",
            "admin-user",
            "read");

        policy.requireRecipientScope(
            NotificationRecipientScopeType.ACCOUNT,
            "acc-admin",
            "ADMIN",
            "admin-user",
            "read");

        ApiErrorException missingCustomerScope = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        NotificationRecipientScopeType.CUSTOMER,
                        "missing-customer",
                        "ADMIN",
                        "admin-user",
                        "read"));
        assertEquals("NOTIFICATION_SCOPE_NOT_FOUND", missingCustomerScope.getCode());
        assertEquals("recipientScopeId", missingCustomerScope.getField());

        ApiErrorException missingAccountScope = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        NotificationRecipientScopeType.ACCOUNT,
                        "missing-account",
                        "ADMIN",
                        "admin-user",
                        "read"));
        assertEquals("NOTIFICATION_SCOPE_NOT_FOUND", missingAccountScope.getCode());
        assertEquals("recipientScopeId", missingAccountScope.getField());

        policy.requireRecipientScope(
                NotificationRecipientScopeType.ADMIN,
                "virtual-admin",
                "ADMIN",
                "admin-user",
                "read");
    }

    @Test
    void requireRecipientScopeForCustomerRoleCoversCustomerBranchOutcomes() {
        CustomerEntity customer = customer("cust-1", "owner-1", "creator-1");
        customers.put("cust-1", customer);

        ApiErrorException forbiddenWithNullActor = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        NotificationRecipientScopeType.CUSTOMER,
                        "cust-1",
                        "CUSTOMER",
                        null,
                        "read"));
        assertEquals("NOTIFICATION_FORBIDDEN", forbiddenWithNullActor.getCode());

        ApiErrorException forbiddenWithBlankActor = assertThrows(
            ApiErrorException.class,
            () -> policy.requireRecipientScope(
                NotificationRecipientScopeType.CUSTOMER,
                "cust-1",
                "CUSTOMER",
                "   ",
                "read"));
        assertEquals("NOTIFICATION_FORBIDDEN", forbiddenWithBlankActor.getCode());

        policy.requireRecipientScope(
                NotificationRecipientScopeType.CUSTOMER,
                "cust-1",
                "CUSTOMER",
                " owner-1 ",
                "read");

        policy.requireRecipientScope(
                NotificationRecipientScopeType.CUSTOMER,
                "cust-1",
                "CUSTOMER",
                " creator-1 ",
                "read");

            ApiErrorException missingCustomerScope = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                    NotificationRecipientScopeType.CUSTOMER,
                    "missing-customer",
                    "CUSTOMER",
                    "owner-1",
                    "read"));
            assertEquals("NOTIFICATION_SCOPE_NOT_FOUND", missingCustomerScope.getCode());
            assertEquals("recipientScopeId", missingCustomerScope.getField());
            }

            @Test
            void requireRecipientScopeForCustomerRoleCoversAccountBranchOutcomes() {
            AccountEntity directAccount = account("acc-direct", "cust-3", "direct-owner", "direct-creator");
            accounts.put("acc-direct", directAccount);

            policy.requireRecipientScope(
                NotificationRecipientScopeType.ACCOUNT,
                "acc-direct",
                "CUSTOMER",
                " direct-owner ",
                "read");

            policy.requireRecipientScope(
                NotificationRecipientScopeType.ACCOUNT,
                "acc-direct",
                "CUSTOMER",
                "direct-creator",
                "read");

        AccountEntity account = account("acc-1", "cust-2", "other-owner", "other-creator");
        accounts.put("acc-1", account);
        customers.put("cust-2", customer("cust-2", "customer-owner", "customer-creator"));

        policy.requireRecipientScope(
                NotificationRecipientScopeType.ACCOUNT,
                "acc-1",
                "CUSTOMER",
                "customer-owner",
                "read");

        ApiErrorException missingAccountScope = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        NotificationRecipientScopeType.ACCOUNT,
                        "missing-account",
                        "CUSTOMER",
                        "owner-1",
                        "read"));
        assertEquals("NOTIFICATION_SCOPE_NOT_FOUND", missingAccountScope.getCode());
        assertEquals("recipientScopeId", missingAccountScope.getField());
    }

    @Test
    void requireRecipientScopeForbidsCustomerOnAdminScope() {
        ApiErrorException forbidden = assertThrows(
                ApiErrorException.class,
                () -> policy.requireRecipientScope(
                        NotificationRecipientScopeType.ADMIN,
                        "virtual-admin",
                        "CUSTOMER",
                        "actor-1",
                        "read"));

        assertEquals("NOTIFICATION_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void requireRecipientScopeHitsDefaultValidationWhenSwitchMapUnexpected() {
        customers.put("cust-switch-default", customer("cust-switch-default", "owner-switch", "creator-switch"));

        withCorruptedSwitchMapping(NotificationRecipientScopeType.CUSTOMER, () -> {
            ApiErrorException invalidScopeType = assertThrows(
                    ApiErrorException.class,
                    () -> policy.requireRecipientScope(
                            NotificationRecipientScopeType.CUSTOMER,
                            "cust-switch-default",
                            "CUSTOMER",
                            "owner-switch",
                            "read"));

            assertEquals("NOTIFICATION_VALIDATION_ERROR", invalidScopeType.getCode());
            assertEquals("recipientScopeType", invalidScopeType.getField());
        });
    }

    @Test
    void ensureScopeExistsHitsDefaultValidationWhenSwitchMapUnexpected() {
        customers.put("cust-switch-default-admin", customer("cust-switch-default-admin", "owner-admin", "creator-admin"));

        withCorruptedSwitchMapping(NotificationRecipientScopeType.CUSTOMER, () -> {
            ApiErrorException invalidScopeType = assertThrows(
                    ApiErrorException.class,
                    () -> policy.requireRecipientScope(
                            NotificationRecipientScopeType.CUSTOMER,
                            "cust-switch-default-admin",
                            "ADMIN",
                            "admin-user",
                            "read"));

            assertEquals("NOTIFICATION_VALIDATION_ERROR", invalidScopeType.getCode());
            assertEquals("recipientScopeType", invalidScopeType.getField());
        });
    }

    @Test
    void requireEventScopeThrowsWhenEventMissingAndDelegatesOtherwise() {
        ApiErrorException notFound = assertThrows(
                ApiErrorException.class,
                () -> policy.requireEventScope(null, "ADMIN", "actor-1", "read"));
        assertEquals("NOTIFICATION_EVENT_NOT_FOUND", notFound.getCode());

        NotificationEventEntity event = new NotificationEventEntity();
        event.setRecipientScopeType(NotificationRecipientScopeType.ADMIN);
        event.setRecipientScopeId("virtual-admin");

        policy.requireEventScope(event, "ADMIN", "actor-1", "read");
    }

    @Test
    void proxyRepositoriesCoverFindBooleanOptionalAndDefaultBranches() {
        AccountJpaRepository accountJpaRepository = accountRepository();
        CustomerJpaRepository customerJpaRepository = customerRepository();

        accounts.put("acc-proxy", account("acc-proxy", "cust-proxy", "owner-proxy", "creator-proxy"));
        customers.put("cust-proxy", customer("cust-proxy", "owner-proxy", "creator-proxy"));

        assertTrue(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("acc-proxy").isPresent());
        assertTrue(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("missing-account").isEmpty());
        assertFalse(accountJpaRepository.existsByCustomerId("cust-proxy"));
        assertTrue(accountJpaRepository.findByAccountIdAndDeletedAtIsNullForUpdate("acc-proxy").isEmpty());
        assertNull(accountJpaRepository.findByDeletedAtIsNull());

        assertTrue(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("cust-proxy").isPresent());
        assertTrue(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("missing-customer").isEmpty());
        assertFalse(customerJpaRepository.existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull("a@b.com"));
        assertTrue(customerJpaRepository.findById("missing-customer").isEmpty());
        assertNull(customerJpaRepository.findAll());
    }

    @Test
    void resolveNotificationScopeSwitchMapCoversFieldFallbackAndErrorBranches() {
        int[] fromField = resolveNotificationScopeSwitchMap(SwitchMapFieldOnlyHolder.class);
        assertEquals(3, fromField.length);
        assertEquals(7, fromField[0]);

        AssertionError missingMember = assertThrows(
                AssertionError.class,
                () -> resolveNotificationScopeSwitchMap(NotificationAccessPolicyTest.class));
        assertTrue(missingMember.getCause() instanceof NoSuchFieldException);

        AssertionError nullField = assertThrows(
                AssertionError.class,
                () -> resolveNotificationScopeSwitchMap(SwitchMapNullFieldHolder.class));
        assertTrue(nullField.getCause() instanceof NoSuchFieldException);
    }

    @Test
    void invokePrivateBooleanWrapsReflectionErrors() {
        AssertionError assertionError = assertThrows(
                AssertionError.class,
                () -> invokePrivateBoolean(policy, "missingMethod", new Class<?>[0]));

        assertTrue(assertionError.getCause() instanceof ReflectiveOperationException);
    }

    @Test
    void ownershipHelpersReturnFalseForNullInputs() {
        AccountEntity account = account("acc-2", "cust-2", "owner-2", "creator-2");
        CustomerEntity customer = customer("cust-2", "owner-2", "creator-2");

        assertEquals(false, invokePrivateBoolean(
                policy,
                "isAccountOwnedByActor",
                new Class<?>[] { AccountEntity.class, String.class },
                null,
                "actor"));

        assertEquals(false, invokePrivateBoolean(
                policy,
                "isAccountOwnedByActor",
                new Class<?>[] { AccountEntity.class, String.class },
                account,
                null));

        assertEquals(false, invokePrivateBoolean(
                policy,
                "isAccountOwnedByActor",
                new Class<?>[] { AccountEntity.class, String.class },
                account,
                "   "));

        assertEquals(true, invokePrivateBoolean(
                policy,
                "isAccountOwnedByActor",
                new Class<?>[] { AccountEntity.class, String.class },
                account,
                "creator-2"));

        assertEquals(false, invokePrivateBoolean(
                policy,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                null,
                "actor"));

        assertEquals(false, invokePrivateBoolean(
                policy,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                customer,
                null));

        assertEquals(false, invokePrivateBoolean(
                policy,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                customer,
                "   "));

        assertEquals(true, invokePrivateBoolean(
                policy,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                customer,
                "creator-2"));
    }

    private void withCorruptedSwitchMapping(NotificationRecipientScopeType scopeType, Runnable assertion) {
        int[] switchMap = resolveNotificationScopeSwitchMap();
        int index = scopeType.ordinal();
        int previous = switchMap[index];

        switchMap[index] = 0;
        try {
            assertion.run();
        } finally {
            switchMap[index] = previous;
        }
    }

    private int[] resolveNotificationScopeSwitchMap() {
        return resolveNotificationScopeSwitchMap(NotificationAccessPolicy.class);
    }

    private int[] resolveNotificationScopeSwitchMap(Class<?> targetClass) {
        try {
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().contains("$SWITCH_TABLE$com$example$banking$models$NotificationRecipientScopeType")
                        && method.getParameterCount() == 0
                        && method.getReturnType().equals(int[].class)) {
                    method.setAccessible(true);
                    return (int[]) method.invoke(null);
                }
            }

            for (Field field : targetClass.getDeclaredFields()) {
                if (field.getName().contains("$SWITCH_TABLE$com$example$banking$models$NotificationRecipientScopeType")
                        && field.getType().equals(int[].class)) {
                    field.setAccessible(true);
                    int[] current = (int[]) field.get(null);
                    if (current != null) {
                        return current;
                    }
                }
            }

            throw new NoSuchFieldException("No NotificationRecipientScopeType switch-map member found");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private AccountJpaRepository accountRepository() {
        return (AccountJpaRepository) Proxy.newProxyInstance(
                AccountJpaRepository.class.getClassLoader(),
                new Class<?>[] { AccountJpaRepository.class },
                (proxy, method, args) -> {
                    if ("findByAccountIdAndDeletedAtIsNull".equals(method.getName())) {
                        return Optional.ofNullable(accounts.get((String) args[0]));
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (Optional.class.equals(method.getReturnType())) {
                        return Optional.empty();
                    }
                    return null;
                });
    }

    private CustomerJpaRepository customerRepository() {
        return (CustomerJpaRepository) Proxy.newProxyInstance(
                CustomerJpaRepository.class.getClassLoader(),
                new Class<?>[] { CustomerJpaRepository.class },
                (proxy, method, args) -> {
                    if ("findByCustomerIdAndDeletedAtIsNull".equals(method.getName())) {
                        return Optional.ofNullable(customers.get((String) args[0]));
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (Optional.class.equals(method.getReturnType())) {
                        return Optional.empty();
                    }
                    return null;
                });
    }

    private AccountEntity account(String accountId, String customerId, String ownerUserId, String createdByUserId) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCustomerId(customerId);
        account.setOwnerUserId(ownerUserId);
        account.setCreatedByUserId(createdByUserId);
        return account;
    }

    private CustomerEntity customer(String customerId, String ownerUserId, String createdByUserId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId(customerId);
        customer.setOwnerUserId(ownerUserId);
        customer.setCreatedByUserId(createdByUserId);
        return customer;
    }

    private boolean invokePrivateBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            Object result = method.invoke(target, args);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class SwitchMapFieldOnlyHolder {
        @SuppressWarnings("unused")
        private static int[] $SWITCH_TABLE$com$example$banking$models$NotificationRecipientScopeType = new int[] { 7, 8, 9 };
    }

    private static final class SwitchMapNullFieldHolder {
        @SuppressWarnings("unused")
        private static int[] $SWITCH_TABLE$com$example$banking$models$NotificationRecipientScopeType;
    }
}