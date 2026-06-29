package com.example.banking.api.standingorders.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.example.banking.api.standingorders.StandingOrderResponseMapper;
import com.example.banking.api.standingorders.schemas.StandingOrderListResponseSchema;
import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.ListStandingOrdersService;
import com.example.banking.services.StandingOrderRepository;

class ListStandingOrdersRouteTest {

    @Test
    void listReturnsMappedPageUsingResolvedPrincipalScope() {
    CapturingStandingOrderRepository repository = new CapturingStandingOrderRepository(samplePage());
    StandingOrderModuleConfig config = new StandingOrderModuleConfig();
    config.setMaxPageSize(50);

    ListStandingOrdersService service = new ListStandingOrdersService(
        repository,
        new StandingOrderAccessPolicy(null, null),
        config);
    ListStandingOrdersRoute route = new ListStandingOrdersRoute(
        service,
        new CustomerPrincipalResolver(),
        new StandingOrderResponseMapper());

    TestingAuthenticationToken authentication = new TestingAuthenticationToken("user-1", "credentials", "ROLE_customer");
    authentication.setAuthenticated(true);

    var response = route.list(1, 20, authentication);
        StandingOrderListResponseSchema body = response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, body.items().size());
        assertEquals("so-1", body.items().get(0).standingOrderId());
        assertEquals(1, body.page());
        assertEquals(20, body.pageSize());
        assertEquals(1L, body.totalItems());
        assertEquals(1, body.totalPages());
        assertEquals("user-1", repository.lastActorUserId);
        assertEquals("CUSTOMER", repository.lastRole);
        assertEquals(1, repository.lastPage);
        assertEquals(20, repository.lastPageSize);
    }

    @Test
    void listClampsInvalidAndOversizedPageInputs() {
        CapturingStandingOrderRepository repository = new CapturingStandingOrderRepository(new PageImpl<>(List.of()));
        StandingOrderModuleConfig config = new StandingOrderModuleConfig();
        config.setMaxPageSize(25);

        ListStandingOrdersService service = new ListStandingOrdersService(
                repository,
                new StandingOrderAccessPolicy(null, null),
                config);
        ListStandingOrdersRoute route = new ListStandingOrdersRoute(
                service,
                new CustomerPrincipalResolver(),
                new StandingOrderResponseMapper());

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user-2", "credentials", "ROLE_CUSTOMER");
        authentication.setAuthenticated(true);

        var response = route.list(0, 999, authentication);
        StandingOrderListResponseSchema body = response.getBody();

        assertEquals("user-2", repository.lastActorUserId);
        assertEquals("CUSTOMER", repository.lastRole);
        assertEquals(1, repository.lastPage);
        assertEquals(25, repository.lastPageSize);
        assertEquals(1, body.page());
        assertEquals(999, body.pageSize());
        assertEquals(1, body.totalPages());
    }

    private Page<StandingOrderEntity> samplePage() {
        StandingOrderEntity entity = new StandingOrderEntity();
        entity.setStandingOrderId("so-1");
        entity.setSourceAccountId("acc-1");
        entity.setDestinationAccountId("acc-2");
        entity.setAmount(new BigDecimal("12.34"));
        entity.setCadence(StandingOrderCadence.MONTHLY);
        entity.setLifecycleState(StandingOrderLifecycleState.ACTIVE);
        entity.setNextExecutionAtUtc(Instant.parse("2026-07-01T00:00:00Z"));
        entity.setEffectiveFromUtc(Instant.parse("2026-06-01T00:00:00Z"));
        entity.setEffectiveToUtc(null);
        return new PageImpl<>(List.of(entity));
    }

    private static final class CapturingStandingOrderRepository implements StandingOrderRepository {
        private final Page<StandingOrderEntity> pageToReturn;
        private String lastActorUserId;
        private String lastRole;
        private int lastPage;
        private int lastPageSize;

        private CapturingStandingOrderRepository(Page<StandingOrderEntity> pageToReturn) {
            this.pageToReturn = pageToReturn;
        }

        @Override
        public StandingOrderEntity save(StandingOrderEntity standingOrder) {
            return standingOrder;
        }

        @Override
        public Optional<StandingOrderEntity> findById(String standingOrderId) {
            return Optional.empty();
        }

        @Override
        public List<StandingOrderEntity> findDueWithinWindow(Instant windowStartUtc, Instant windowEndUtc) {
            return List.of();
        }

        @Override
        public Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize) {
            this.lastActorUserId = actorUserId;
            this.lastRole = role;
            this.lastPage = page;
            this.lastPageSize = pageSize;
            return pageToReturn;
        }
    }
}
