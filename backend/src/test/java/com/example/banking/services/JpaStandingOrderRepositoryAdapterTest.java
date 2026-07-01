package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.banking.lib.StandingOrderJpaRepository;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

class JpaStandingOrderRepositoryAdapterTest {

    private CapturingStandingOrderJpaRepository capturingRepository;
    private JpaStandingOrderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        capturingRepository = new CapturingStandingOrderJpaRepository();
        adapter = new JpaStandingOrderRepositoryAdapter(capturingRepository.proxy());
    }

    @Test
    void saveDelegatesToJpaRepository() {
        StandingOrderEntity entity = new StandingOrderEntity();
        entity.setStandingOrderId("standing-order-1");
        capturingRepository.saveResult = entity;

        StandingOrderEntity saved = adapter.save(entity);

        assertSame(entity, capturingRepository.lastSavedEntity);
        assertSame(entity, saved);
    }

    @Test
    void findByIdDelegatesToJpaRepository() {
        StandingOrderEntity entity = new StandingOrderEntity();
        entity.setStandingOrderId("standing-order-2");
        capturingRepository.findByIdResult = Optional.of(entity);

        Optional<StandingOrderEntity> found = adapter.findById("standing-order-2");

        assertEquals("standing-order-2", capturingRepository.lastFindById);
        assertSame(entity, found.orElseThrow());
    }

    @Test
    void findDueWithinWindowUsesActiveStateAndDelegatesWindow() {
        Instant windowStart = Instant.parse("2026-07-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-07-01T23:59:59Z");

        StandingOrderEntity due = new StandingOrderEntity();
        due.setStandingOrderId("standing-order-3");
        capturingRepository.findDueResult = List.of(due);

        List<StandingOrderEntity> dueOrders = adapter.findDueWithinWindow(windowStart, windowEnd);

        assertEquals(StandingOrderLifecycleState.ACTIVE, capturingRepository.lastLifecycleState);
        assertEquals(windowStart, capturingRepository.lastWindowStart);
        assertEquals(windowEnd, capturingRepository.lastWindowEnd);
        assertSame(due, dueOrders.get(0));
    }

    @Test
    void listByScopeNormalizesPageAndKeepsInRangePageSize() {
        Page<StandingOrderEntity> expected = new PageImpl<>(List.of(new StandingOrderEntity()));
        capturingRepository.listByScopeResult = expected;

        Page<StandingOrderEntity> result = adapter.listByScope("actor-1", "CUSTOMER", 1, 25);

        assertSame(expected, result);
        assertEquals("actor-1", capturingRepository.lastActorUserId);
        assertEquals("CUSTOMER", capturingRepository.lastRole);
        assertEquals(0, capturingRepository.lastPageable.getPageNumber());
        assertEquals(25, capturingRepository.lastPageable.getPageSize());

        Sort.Order order = capturingRepository.lastPageable.getSort().getOrderFor("updatedAtUtc");
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void listByScopeNormalizesLowValuesToMinimums() {
        adapter.listByScope("actor-2", "ADMIN", 0, 0);

        assertEquals(0, capturingRepository.lastPageable.getPageNumber());
        assertEquals(1, capturingRepository.lastPageable.getPageSize());
    }

    @Test
    void listByScopeNormalizesLargeValuesToUpperBound() {
        adapter.listByScope("actor-3", "CUSTOMER", 5, 500);

        assertEquals(4, capturingRepository.lastPageable.getPageNumber());
        assertEquals(100, capturingRepository.lastPageable.getPageSize());
    }

    private static final class CapturingStandingOrderJpaRepository {
        StandingOrderEntity saveResult;
        Optional<StandingOrderEntity> findByIdResult = Optional.empty();
        List<StandingOrderEntity> findDueResult = List.of();
        Page<StandingOrderEntity> listByScopeResult = Page.empty();

        StandingOrderEntity lastSavedEntity;
        String lastFindById;

        StandingOrderLifecycleState lastLifecycleState;
        Instant lastWindowStart;
        Instant lastWindowEnd;

        String lastActorUserId;
        String lastRole;
        Pageable lastPageable;

        private final StandingOrderJpaRepository proxy = (StandingOrderJpaRepository) Proxy.newProxyInstance(
                StandingOrderJpaRepository.class.getClassLoader(),
                new Class<?>[] { StandingOrderJpaRepository.class },
                (obj, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        lastSavedEntity = (StandingOrderEntity) args[0];
                        yield saveResult;
                    }
                    case "findByStandingOrderId" -> {
                        lastFindById = (String) args[0];
                        yield findByIdResult;
                    }
                    case "findDueWithinWindow" -> {
                        lastLifecycleState = (StandingOrderLifecycleState) args[0];
                        lastWindowStart = (Instant) args[1];
                        lastWindowEnd = (Instant) args[2];
                        yield findDueResult;
                    }
                    case "listByScope" -> {
                        lastActorUserId = (String) args[0];
                        lastRole = (String) args[1];
                        lastPageable = (Pageable) args[2];
                        yield listByScopeResult;
                    }
                    case "toString" -> "CapturingStandingOrderJpaRepositoryProxy";
                    case "hashCode" -> System.identityHashCode(obj);
                    case "equals" -> obj == args[0];
                    default -> throw new UnsupportedOperationException("Unhandled method: " + method.getName());
                });

        StandingOrderJpaRepository proxy() {
            return proxy;
        }
    }
}
