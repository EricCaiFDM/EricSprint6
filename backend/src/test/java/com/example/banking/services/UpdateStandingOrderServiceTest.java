package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.standingorders.schemas.UpdateStandingOrderSchema;
import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.StandingOrderLifecycleEventJpaRepository;
import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.scheduling.StandingOrderScheduleCalculator;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleEventEntity;
import com.example.banking.models.StandingOrderLifecycleState;

class UpdateStandingOrderServiceTest {

    private final InMemoryStandingOrderRepository repository = new InMemoryStandingOrderRepository();
    private final InMemoryLifecycleEventRepository eventRepository = new InMemoryLifecycleEventRepository();
    private UpdateStandingOrderService service;

    @BeforeEach
    void setUp() {
        StandingOrderModuleConfig standingOrderModuleConfig = new StandingOrderModuleConfig();
        standingOrderModuleConfig.setDefaultRetryPolicyCode("STANDARD");

        TransactionModuleConfig transactionModuleConfig = new TransactionModuleConfig();
        transactionModuleConfig.setDefaultCurrencyCode("USD");

        service = new UpdateStandingOrderService(
                repository,
                new StandingOrderAccessPolicy(accountRepository(), customerRepository()),
                new StandingOrderLifecyclePolicyService(),
                new StandingOrderScheduleCalculator(),
                new StandingOrderLifecycleAuditService(eventRepository),
                new MoneyPolicy(transactionModuleConfig),
                standingOrderModuleConfig);
    }

    @Test
    void updateAppliesMutableFieldsAndWritesAuditEvent() {
        String id = UUID.randomUUID().toString();
        repository.put(baseStandingOrder(id));

        UpdateStandingOrderSchema request = new UpdateStandingOrderSchema(
                "99.99",
                "WEEKLY",
                "2026-07-01T00:00:00Z",
                "2026-12-31T00:00:00Z",
                "no_retry");

        StandingOrderEntity updated = service.update(id, request, "owner-1", "CUSTOMER");

        assertEquals(new BigDecimal("99.99"), updated.getAmount());
        assertEquals(StandingOrderCadence.WEEKLY, updated.getCadence());
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), updated.getEffectiveFromUtc());
        assertEquals(Instant.parse("2026-12-31T00:00:00Z"), updated.getEffectiveToUtc());
        assertEquals("NO_RETRY", updated.getRetryPolicyCode());
        assertNotNull(updated.getNextExecutionAtUtc());

        assertEquals(1, eventRepository.saved.size());
        StandingOrderLifecycleEventEntity event = eventRepository.saved.get(0);
        assertEquals(id, event.getStandingOrderId());
        assertEquals("UPDATED", event.getEventType());
        assertEquals("owner-1", event.getActorUserId());
        assertEquals("CUSTOMER", event.getActorRole());
    }

    @Test
    void updateCanClearEffectiveToWithBlankValue() {
        String id = UUID.randomUUID().toString();
        StandingOrderEntity standingOrder = baseStandingOrder(id);
        standingOrder.setEffectiveToUtc(Instant.parse("2026-08-01T00:00:00Z"));
        repository.put(standingOrder);

        UpdateStandingOrderSchema request = new UpdateStandingOrderSchema(
                null,
                null,
                null,
                "   ",
                null);

        StandingOrderEntity updated = service.update(id, request, "owner-1", "CUSTOMER");

        assertNull(updated.getEffectiveToUtc());
    }

    @Test
    void updateMarksOrderCompletedWhenNoFutureExecutionRemains() {
        String id = UUID.randomUUID().toString();
        StandingOrderEntity standingOrder = baseStandingOrder(id);
        standingOrder.setEffectiveFromUtc(Instant.parse("2026-01-01T00:00:00Z"));
        standingOrder.setEffectiveToUtc(Instant.parse("2026-01-02T00:00:00Z"));
        repository.put(standingOrder);

        UpdateStandingOrderSchema request = new UpdateStandingOrderSchema(
                "10.00",
                null,
                null,
                null,
                null);

        StandingOrderEntity updated = service.update(id, request, "owner-1", "CUSTOMER");

        assertEquals(StandingOrderLifecycleState.COMPLETED, updated.getLifecycleState());
        assertNull(updated.getNextExecutionAtUtc());
    }

    @Test
    void updateUsesDefaultRetryPolicyWhenBlankRetryProvided() {
        String id = UUID.randomUUID().toString();
        repository.put(baseStandingOrder(id));

        UpdateStandingOrderSchema request = new UpdateStandingOrderSchema(
                "10.00",
                null,
                null,
                null,
                " ");

        StandingOrderEntity updated = service.update(id, request, "owner-1", "CUSTOMER");
        assertEquals("STANDARD", updated.getRetryPolicyCode());
    }

    @Test
    void updateValidatesIdentifierAndRejectsMissingMutations() {
        ApiErrorException invalidId = captureUpdateError(
            "bad-id",
            new UpdateStandingOrderSchema(null, null, null, null, null),
            "owner-1",
            "CUSTOMER");
        assertEquals("standingOrderId", invalidId.getField());

        String id = UUID.randomUUID().toString();
        repository.put(baseStandingOrder(id));

        ApiErrorException noFields = captureUpdateError(
            id,
            new UpdateStandingOrderSchema(null, null, null, null, null),
            "owner-1",
            "CUSTOMER");
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", noFields.getCode());
    }

    @Test
    void updateValidatesCadenceAndInstantFormats() {
        String id = UUID.randomUUID().toString();
        repository.put(baseStandingOrder(id));

        ApiErrorException cadenceError = captureUpdateError(
            id,
            new UpdateStandingOrderSchema("10.00", "YEARLY", null, null, null),
            "owner-1",
            "CUSTOMER");
        assertEquals("cadence", cadenceError.getField());

        ApiErrorException fromError = captureUpdateError(
            id,
            new UpdateStandingOrderSchema("10.00", null, "bad", null, null),
            "owner-1",
            "CUSTOMER");
        assertEquals("effectiveFromUtc", fromError.getField());

        ApiErrorException toError = captureUpdateError(
            id,
            new UpdateStandingOrderSchema("10.00", null, null, "bad", null),
            "owner-1",
            "CUSTOMER");
        assertEquals("effectiveToUtc", toError.getField());
    }

        private ApiErrorException captureUpdateError(
            String standingOrderId,
            UpdateStandingOrderSchema request,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.update(standingOrderId, request, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
        }

    @Test
    void proxyRepositoriesCoverAllInvocationHandlerBranches() {
        AccountJpaRepository accountJpaRepository = accountRepository();

        AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNull("acc-1").orElseThrow();
        assertEquals("acc-1", account.getAccountId());
        assertEquals("owner-1", account.getOwnerUserId());
        assertFalse(accountJpaRepository.existsByCustomerId("cust-1"));
        assertNull(accountJpaRepository.save(new AccountEntity()));

        CustomerJpaRepository customerJpaRepository = customerRepository();
        assertTrue(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("cust-1").isEmpty());
        assertFalse(customerJpaRepository.existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull("a@bank.com"));
        assertEquals(0, customerJpaRepository.saveAll(List.of()).size());
    }

    @Test
    void inMemoryStandingOrderRepositoryDefaultMethodsAreCovered() {
        InMemoryStandingOrderRepository inMemoryRepository = new InMemoryStandingOrderRepository();
        String id = UUID.randomUUID().toString();
        StandingOrderEntity standingOrder = baseStandingOrder(id);

        assertSame(standingOrder, inMemoryRepository.save(standingOrder));
        assertSame(standingOrder, inMemoryRepository.findById(id).orElseThrow());
        assertTrue(inMemoryRepository.findById(UUID.randomUUID().toString()).isEmpty());
        assertTrue(inMemoryRepository.findDueWithinWindow(Instant.now(), Instant.now().plusSeconds(60)).isEmpty());
        assertTrue(inMemoryRepository.listByScope("owner-1", "CUSTOMER", 1, 10).isEmpty());
    }

    @Test
    void inMemoryLifecycleEventRepositoryMethodsAreCovered() {
        InMemoryLifecycleEventRepository inMemoryRepository = new InMemoryLifecycleEventRepository();

        StandingOrderLifecycleEventEntity event1 = lifecycleEvent("so-1", "CREATED");
        StandingOrderLifecycleEventEntity event2 = lifecycleEvent("so-2", "UPDATED");
        StandingOrderLifecycleEventEntity event3 = lifecycleEvent("so-1", "SUSPENDED");

        assertSame(event1, inMemoryRepository.save(event1));
        List<StandingOrderLifecycleEventEntity> savedBatch = inMemoryRepository.saveAll(List.of(event2));
        assertEquals(1, savedBatch.size());
        assertSame(event2, savedBatch.get(0));

        assertTrue(inMemoryRepository.findById("missing").isEmpty());
        assertFalse(inMemoryRepository.existsById("missing"));
        assertEquals(2, inMemoryRepository.findAll().size());
        assertTrue(inMemoryRepository.findAllById(List.of(event1.getEventId())).isEmpty());
        assertEquals(2, inMemoryRepository.count());

        inMemoryRepository.deleteById("missing");
        inMemoryRepository.delete(event2);
        assertEquals(1, inMemoryRepository.count());

        inMemoryRepository.saveAndFlush(event2);
        assertEquals(2, inMemoryRepository.count());

        List<StandingOrderLifecycleEventEntity> flushedBatch = inMemoryRepository.saveAllAndFlush(List.of(event3));
        assertEquals(1, flushedBatch.size());
        assertSame(event3, flushedBatch.get(0));
        assertEquals(3, inMemoryRepository.count());

        List<StandingOrderLifecycleEventEntity> standingOrderEvents =
                inMemoryRepository.findByStandingOrderIdOrderByOccurredAtUtcDesc("so-1");
        assertEquals(2, standingOrderEvents.size());

        inMemoryRepository.flush();
        inMemoryRepository.deleteAllById(List.of(event1.getEventId()));
        inMemoryRepository.deleteAll(List.of(event1));
        assertEquals(2, inMemoryRepository.count());

        inMemoryRepository.deleteAllInBatch(List.of(event2));
        assertEquals(1, inMemoryRepository.count());

        inMemoryRepository.deleteAllByIdInBatch(List.of(event3.getEventId()));
        assertEquals(1, inMemoryRepository.count());

        assertNull(inMemoryRepository.getOne("missing"));
        assertNull(inMemoryRepository.getById("missing"));
        assertNull(inMemoryRepository.getReferenceById("missing"));

        org.springframework.data.domain.Example<StandingOrderLifecycleEventEntity> example =
                org.springframework.data.domain.Example.of(event3);
        assertTrue(inMemoryRepository.findOne(example).isEmpty());
        assertTrue(inMemoryRepository.findAll(example).isEmpty());
        assertTrue(inMemoryRepository.findAll(example, org.springframework.data.domain.Sort.unsorted()).isEmpty());
        assertTrue(inMemoryRepository.findAll(example, org.springframework.data.domain.Pageable.unpaged()).isEmpty());
        assertEquals(0, inMemoryRepository.count(example));
        assertFalse(inMemoryRepository.exists(example));

        assertEquals(1, inMemoryRepository.findAll(org.springframework.data.domain.Sort.unsorted()).size());
        assertTrue(inMemoryRepository.findAll(org.springframework.data.domain.Pageable.unpaged()).isEmpty());

        inMemoryRepository.deleteAllInBatch();
        assertEquals(0, inMemoryRepository.count());

        inMemoryRepository.save(event1);
        assertEquals(1, inMemoryRepository.count());
        inMemoryRepository.deleteAll();
        assertEquals(0, inMemoryRepository.count());
    }

    private StandingOrderLifecycleEventEntity lifecycleEvent(String standingOrderId, String eventType) {
        StandingOrderLifecycleEventEntity event = new StandingOrderLifecycleEventEntity();
        event.setEventId(UUID.randomUUID().toString());
        event.setStandingOrderId(standingOrderId);
        event.setEventType(eventType);
        event.setActorUserId("owner-1");
        event.setActorRole("CUSTOMER");
        event.setOccurredAtUtc(Instant.now());
        event.setMetadata("{}");
        return event;
    }

    private StandingOrderEntity baseStandingOrder(String id) {
        StandingOrderEntity standingOrder = new StandingOrderEntity();
        standingOrder.setStandingOrderId(id);
        standingOrder.setSourceAccountId("acc-1");
        standingOrder.setDestinationAccountId("acc-2");
        standingOrder.setAmount(new BigDecimal("10.00"));
        standingOrder.setCurrencyCode("USD");
        standingOrder.setCadence(StandingOrderCadence.DAILY);
        standingOrder.setScheduleConfig("{}");
        standingOrder.setEffectiveFromUtc(Instant.parse("2026-06-28T00:00:00Z"));
        standingOrder.setEffectiveToUtc(Instant.parse("2026-07-30T00:00:00Z"));
        standingOrder.setNextExecutionAtUtc(Instant.parse("2026-06-29T00:00:00Z"));
        standingOrder.setLifecycleState(StandingOrderLifecycleState.ACTIVE);
        standingOrder.setRetryPolicyCode("STANDARD");
        standingOrder.setCreatedByUserId("owner-1");
        standingOrder.setUpdatedAtUtc(Instant.parse("2026-06-29T00:00:00Z"));
        return standingOrder;
    }

    private AccountJpaRepository accountRepository() {
        return (AccountJpaRepository) java.lang.reflect.Proxy.newProxyInstance(
                AccountJpaRepository.class.getClassLoader(),
                new Class<?>[] { AccountJpaRepository.class },
                (proxy, method, args) -> {
                    if ("findByAccountIdAndDeletedAtIsNull".equals(method.getName())) {
                        AccountEntity account = new AccountEntity();
                        account.setAccountId((String) args[0]);
                        account.setCustomerId("cust-1");
                        account.setOwnerUserId("owner-1");
                        account.setCreatedByUserId("owner-1");
                        return Optional.of(account);
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                });
    }

    private CustomerJpaRepository customerRepository() {
        return (CustomerJpaRepository) java.lang.reflect.Proxy.newProxyInstance(
                CustomerJpaRepository.class.getClassLoader(),
                new Class<?>[] { CustomerJpaRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("find")) {
                        return Optional.empty();
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return List.of();
                });
    }

    private static final class InMemoryStandingOrderRepository implements StandingOrderRepository {
        private final Map<String, StandingOrderEntity> storage = new HashMap<>();

        private void put(StandingOrderEntity standingOrder) {
            storage.put(standingOrder.getStandingOrderId(), standingOrder);
        }

        @Override
        public StandingOrderEntity save(StandingOrderEntity standingOrder) {
            storage.put(standingOrder.getStandingOrderId(), standingOrder);
            return standingOrder;
        }

        @Override
        public Optional<StandingOrderEntity> findById(String standingOrderId) {
            return Optional.ofNullable(storage.get(standingOrderId));
        }

        @Override
        public java.util.List<StandingOrderEntity> findDueWithinWindow(Instant windowStartUtc, Instant windowEndUtc) {
            return List.of();
        }

        @Override
        public org.springframework.data.domain.Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize) {
            return org.springframework.data.domain.Page.empty();
        }
    }

    private static final class InMemoryLifecycleEventRepository implements StandingOrderLifecycleEventJpaRepository {
        private final java.util.List<StandingOrderLifecycleEventEntity> saved = new java.util.ArrayList<>();

        @Override
        public <S extends StandingOrderLifecycleEventEntity> S save(S entity) {
            saved.add(entity);
            return entity;
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> java.util.List<S> saveAll(Iterable<S> entities) {
            java.util.List<S> list = new java.util.ArrayList<>();
            for (S entity : entities) {
                list.add(save(entity));
            }
            return list;
        }

        @Override
        public java.util.Optional<StandingOrderLifecycleEventEntity> findById(String s) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(String s) {
            return false;
        }

        @Override
        public java.util.List<StandingOrderLifecycleEventEntity> findAll() {
            return saved;
        }

        @Override
        public java.util.List<StandingOrderLifecycleEventEntity> findAllById(Iterable<String> strings) {
            return List.of();
        }

        @Override
        public long count() {
            return saved.size();
        }

        @Override
        public void deleteById(String s) {
        }

        @Override
        public void delete(StandingOrderLifecycleEventEntity entity) {
            saved.remove(entity);
        }

        @Override
        public void deleteAllById(Iterable<? extends String> strings) {
        }

        @Override
        public void deleteAll(Iterable<? extends StandingOrderLifecycleEventEntity> entities) {
            for (StandingOrderLifecycleEventEntity entity : entities) {
                saved.remove(entity);
            }
        }

        @Override
        public void deleteAll() {
            saved.clear();
        }

        public java.util.List<StandingOrderLifecycleEventEntity> findByStandingOrderIdOrderByOccurredAtUtcDesc(String standingOrderId) {
            return saved.stream().filter(event -> standingOrderId.equals(event.getStandingOrderId())).toList();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> java.util.List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<StandingOrderLifecycleEventEntity> entities) {
            deleteAll(entities);
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<String> strings) {
        }

        @Override
        public void deleteAllInBatch() {
            saved.clear();
        }

        @Override
        public StandingOrderLifecycleEventEntity getOne(String s) {
            return null;
        }

        @Override
        public StandingOrderLifecycleEventEntity getById(String s) {
            return null;
        }

        @Override
        public StandingOrderLifecycleEventEntity getReferenceById(String s) {
            return null;
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
            return Optional.empty();
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) {
            return List.of();
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) {
            return List.of();
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> long count(org.springframework.data.domain.Example<S> example) {
            return 0;
        }

        @Override
        public <S extends StandingOrderLifecycleEventEntity> boolean exists(org.springframework.data.domain.Example<S> example) {
            return false;
        }

        @Override
        public java.util.List<StandingOrderLifecycleEventEntity> findAll(org.springframework.data.domain.Sort sort) {
            return saved;
        }

        @Override
        public org.springframework.data.domain.Page<StandingOrderLifecycleEventEntity> findAll(org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
    }
}
