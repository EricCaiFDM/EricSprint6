package com.example.banking.services;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.banking.api.notifications.schemas.TriggerNotificationSchema;

@Service
public class NotificationAutoTriggerService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationAutoTriggerService.class);

    private final TriggerNotificationService triggerNotificationService;

    public NotificationAutoTriggerService(TriggerNotificationService triggerNotificationService) {
        this.triggerNotificationService = triggerNotificationService;
    }

    public void triggerDepositPosted(String accountId, String amount, String actorUserId, String role) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("operation", "DEPOSIT");
        context.put("accountId", accountId);
        context.put("amount", amount);
        context.put("title", "Deposit posted");

        fire(
                "DEPOSIT_POSTED",
                "ACCOUNT",
                accountId,
                "ACCOUNT_ACTIVITY",
                context,
                actorUserId,
                role);
    }

    public void triggerTransferCompleted(
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String transferId,
            String actorUserId,
            String role) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("operation", "TRANSFER");
        context.put("sourceAccountId", sourceAccountId);
        context.put("destinationAccountId", destinationAccountId);
        context.put("amount", amount);
        context.put("transferId", transferId);
        context.put("title", "Transfer completed");

        fire(
                "TRANSFER_COMPLETED",
                "ACCOUNT",
                sourceAccountId,
                "TRANSFER_ACTIVITY",
                context,
                actorUserId,
                role);
    }

    public void triggerStandingOrderExecuted(
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String standingOrderId,
            String transferId) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("operation", "STANDING_ORDER");
        context.put("sourceAccountId", sourceAccountId);
        context.put("destinationAccountId", destinationAccountId);
        context.put("amount", amount);
        context.put("standingOrderId", standingOrderId);
        context.put("transferId", transferId);
        context.put("title", "Standing order executed");

        fire(
                "STANDING_ORDER_EXECUTED",
                "ACCOUNT",
                sourceAccountId,
                "STANDING_ORDER_ACTIVITY",
                context,
                "system-scheduler",
                "ADMIN");
    }

    private void fire(
            String eventType,
            String recipientScopeType,
            String recipientScopeId,
            String templateCode,
            Map<String, Object> templateContext,
            String actorUserId,
            String role) {
        if (recipientScopeId == null || recipientScopeId.isBlank()) {
            return;
        }

        try {
            TriggerNotificationSchema request = new TriggerNotificationSchema(
                    eventType,
                    recipientScopeType,
                    recipientScopeId,
                    templateCode,
                    templateContext);

            triggerNotificationService.trigger(request, normalizeActor(actorUserId), normalizeRole(role));
        } catch (Exception exception) {
            logger.warn(
                    "Automatic notification trigger failed eventType={} recipientScopeId={} reason={}",
                    eventType,
                    recipientScopeId,
                    exception.getMessage());
        }
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "system-scheduler";
        }
        return actorUserId.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ADMIN";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
