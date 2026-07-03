package com.example.banking.services.statement;

import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.errors.StatementErrors;
import com.example.banking.lib.security.StatementAccessGuard;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.models.statement.StatementAccessPolicy;
import com.example.banking.models.statement.StatementRetrievalEvent;
import com.example.banking.models.statement.StatementRetrievalOutcome;

@Service
public class StatementAuthorizationService {
    private final MonthlyStatementRepository monthlyStatementRepository;
    private final StatementRetrievalEventRepository statementRetrievalEventRepository;
    private final StatementAccessGuard statementAccessGuard;
    private final StatementAccessPolicyService statementAccessPolicyService;

    public StatementAuthorizationService(
            MonthlyStatementRepository monthlyStatementRepository,
            StatementRetrievalEventRepository statementRetrievalEventRepository,
            StatementAccessGuard statementAccessGuard,
            StatementAccessPolicyService statementAccessPolicyService) {
        this.monthlyStatementRepository = monthlyStatementRepository;
        this.statementRetrievalEventRepository = statementRetrievalEventRepository;
        this.statementAccessGuard = statementAccessGuard;
        this.statementAccessPolicyService = statementAccessPolicyService;
    }

    public MonthlyStatement readStatementById(String statementId, String actorUserId, String role) {
        StatementAccessPolicy policy = statementAccessPolicyService.resolve(actorUserId, role);
        statementAccessGuard.enforceRetrievalAccess(policy.role());

        MonthlyStatement statement = monthlyStatementRepository.findById(statementId)
                .orElseThrow(() -> {
                    recordRetrieval(statementId, policy, StatementRetrievalOutcome.DENIED_NOT_FOUND, "STATEMENT_NOT_FOUND");
                    return StatementErrors.notFound(statementId);
                });

        try {
            statementAccessGuard.requireStatementScope(statement, policy.role(), policy.userId(), "read");
            recordRetrieval(statementId, policy, StatementRetrievalOutcome.ALLOWED, null);
            return statement;
        } catch (ApiErrorException exception) {
            recordRetrieval(statementId, policy, StatementRetrievalOutcome.DENIED_PERMISSION, exception.getCode());
            throw exception;
        }
    }

    private void recordRetrieval(
            String statementId,
            StatementAccessPolicy policy,
            StatementRetrievalOutcome outcome,
            String reasonCode) {
        StatementRetrievalEvent event = new StatementRetrievalEvent();
        event.setStatementId(statementId);
        event.setRequesterUserId(policy.userId());
        event.setRequesterRole(policy.role());
        event.setOutcome(outcome);
        event.setReasonCode(reasonCode);
        statementRetrievalEventRepository.save(event);
    }
}
