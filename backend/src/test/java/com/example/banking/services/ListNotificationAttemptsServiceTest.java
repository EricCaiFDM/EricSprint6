package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.config.NotificationModuleConfig;
import com.example.banking.lib.errors.NotificationErrors;
import com.example.banking.lib.security.NotificationAccessPolicy;
import com.example.banking.models.NotificationDispatchAttemptEntity;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationRecipientScopeType;

class ListNotificationAttemptsServiceTest {

    private final Map<String, NotificationEventEntity> events = new ConcurrentHashMap<>();

    private CapturingAccessPolicy accessPolicy;
    private CapturingAttemptRepository attemptRepository;
    private ListNotificationAttemptsService service;

    @BeforeEach
    void setUp() {
        accessPolicy = new CapturingAccessPolicy();
        attemptRepository = new CapturingAttemptRepository();

        NotificationModuleConfig config = new NotificationModuleConfig();
        config.setMaxPageSize(20);

        service = new ListNotificationAttemptsService(
                new InMemoryEventRepository(events),
                attemptRepository,
                accessPolicy,
                config);
    }

    @Test
    void listValidatesNotificationEventIdInput() {
        ApiErrorException missing = assertThrows(
                ApiErrorException.class,
                () -> service.list(null, 1, 10, "actor", "ADMIN"));
        assertEquals("NOTIFICATION_VALIDATION_ERROR", missing.getCode());
        assertEquals("notificationEventId", missing.getField());

        ApiErrorException blank = assertThrows(
            ApiErrorException.class,
            () -> service.list("   ", 1, 10, "actor", "ADMIN"));
        assertEquals("NOTIFICATION_VALIDATION_ERROR", blank.getCode());
        assertEquals("notificationEventId", blank.getField());

        ApiErrorException invalid = assertThrows(
                ApiErrorException.class,
                () -> service.list("not-a-uuid", 1, 10, "actor", "ADMIN"));
        assertEquals("NOTIFICATION_VALIDATION_ERROR", invalid.getCode());
        assertEquals("notificationEventId", invalid.getField());
    }

    @Test
    void listThrowsNotFoundWhenNotificationEventDoesNotExist() {
        String missingId = UUID.randomUUID().toString();

        ApiErrorException notFound = assertThrows(
                ApiErrorException.class,
                () -> service.list(missingId, 1, 10, "actor", "ADMIN"));

        assertEquals("NOTIFICATION_EVENT_NOT_FOUND", notFound.getCode());
    }

    @Test
    void listNormalizesInputsAndDelegatesToPolicyAndRepository() {
        String eventId = UUID.randomUUID().toString();
        events.put(eventId, event(eventId, NotificationRecipientScopeType.CUSTOMER, "scope-1"));

        Page<NotificationDispatchAttemptEntity> page = service.list(
                eventId.toUpperCase(),
                0,
                500,
                " actor-100 ",
                "CUSTOMER");

        assertNotNull(page);
        assertEquals(1, attemptRepository.lastPage);
        assertEquals(20, attemptRepository.lastPageSize);
        assertEquals(eventId, attemptRepository.lastEventId);

        assertEquals(eventId, accessPolicy.lastEvent.getNotificationEventId());
        assertEquals("CUSTOMER", accessPolicy.lastRole);
        assertEquals("actor-100", accessPolicy.lastActor);
        assertEquals("read", accessPolicy.lastOperation);
    }

    @Test
    void listUsesAnonymousActorWhenActorMissing() {
        String eventId = UUID.randomUUID().toString();
        events.put(eventId, event(eventId, NotificationRecipientScopeType.CUSTOMER, "scope-2"));

        service.list(eventId, 2, 5, "   ", "CUSTOMER");

        assertEquals("anonymous", accessPolicy.lastActor);
        assertEquals(2, attemptRepository.lastPage);
        assertEquals(5, attemptRepository.lastPageSize);

        service.list(eventId, 2, 5, null, "CUSTOMER");
        assertEquals("anonymous", accessPolicy.lastActor);
    }

    @Test
    void listPropagatesScopeFailureFromAccessPolicy() {
        String eventId = UUID.randomUUID().toString();
        events.put(eventId, event(eventId, NotificationRecipientScopeType.CUSTOMER, "scope-3"));
        accessPolicy.throwForbidden = true;

        ApiErrorException forbidden = assertThrows(
                ApiErrorException.class,
                () -> service.list(eventId, 1, 10, "actor", "CUSTOMER"));

        assertEquals("NOTIFICATION_FORBIDDEN", forbidden.getCode());
    }

    private NotificationEventEntity event(String id, NotificationRecipientScopeType scopeType, String scopeId) {
        NotificationEventEntity event = new NotificationEventEntity();
        event.setNotificationEventId(id);
        event.setRecipientScopeType(scopeType);
        event.setRecipientScopeId(scopeId);
        return event;
    }

    private static class InMemoryEventRepository implements NotificationEventRepository {
        private final Map<String, NotificationEventEntity> events;

        private InMemoryEventRepository(Map<String, NotificationEventEntity> events) {
            this.events = events;
        }

        @Override
        public NotificationEventEntity save(NotificationEventEntity entity) {
            events.put(entity.getNotificationEventId(), entity);
            return entity;
        }

        @Override
        public Optional<NotificationEventEntity> findById(String notificationEventId) {
            return Optional.ofNullable(events.get(notificationEventId));
        }

        @Override
        public java.util.List<NotificationEventEntity> listRecent(int size) {
            return java.util.List.of();
        }
    }

    private static class CapturingAttemptRepository implements NotificationDispatchAttemptRepository {
        private String lastEventId;
        private int lastPage;
        private int lastPageSize;

        @Override
        public NotificationDispatchAttemptEntity save(NotificationDispatchAttemptEntity entity) {
            return entity;
        }

        @Override
        public Page<NotificationDispatchAttemptEntity> listByEventId(String notificationEventId, int page, int pageSize) {
            this.lastEventId = notificationEventId;
            this.lastPage = page;
            this.lastPageSize = pageSize;
            return new PageImpl<>(java.util.List.of(new NotificationDispatchAttemptEntity()));
        }

        @Override
        public long countByEventId(String notificationEventId) {
            return 0;
        }
    }

    private static class CapturingAccessPolicy extends NotificationAccessPolicy {
        private NotificationEventEntity lastEvent;
        private String lastRole;
        private String lastActor;
        private String lastOperation;
        private boolean throwForbidden;

        private CapturingAccessPolicy() {
            super(null, null);
        }

        @Override
        public void requireEventScope(NotificationEventEntity event, String role, String actorUserId, String operation) {
            this.lastEvent = event;
            this.lastRole = role;
            this.lastActor = actorUserId;
            this.lastOperation = operation;
            if (throwForbidden) {
                throw NotificationErrors.forbidden(operation);
            }
        }
    }
}