package com.example.banking.services;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.banking.api.transactions.mappers.HistoryResponseMapper;
import com.example.banking.api.transactions.schemas.HistorySchema;
import com.example.banking.api.transactions.schemas.TransactionHistoryResponseSchema;
import com.example.banking.lib.security.TransactionAccessPolicy;
import com.example.banking.models.TransactionEntity;

@Service
public class TransactionHistoryService {
    private final TransactionHistoryQueryPolicy queryPolicy;
    private final TransactionAccessPolicy accessPolicy;
    private final TransactionRepository transactionRepository;
    private final HistoryResponseMapper historyResponseMapper;

    public TransactionHistoryService(
            TransactionHistoryQueryPolicy queryPolicy,
            TransactionAccessPolicy accessPolicy,
            TransactionRepository transactionRepository,
            HistoryResponseMapper historyResponseMapper) {
        this.queryPolicy = queryPolicy;
        this.accessPolicy = accessPolicy;
        this.transactionRepository = transactionRepository;
        this.historyResponseMapper = historyResponseMapper;
    }

    public TransactionHistoryResponseSchema getHistory(HistorySchema request, String actorUserId, String role) {
        TransactionHistoryQueryPolicy.QueryPolicyResult normalized = queryPolicy.normalize(request);
        accessPolicy.enforceHistoryScope(normalized.scopeType(), normalized.scopeId(), role, actorUserId);

        Page<TransactionEntity> historyPage;
        if ("ACCOUNT".equals(normalized.scopeType())) {
            historyPage = transactionRepository.findAccountHistory(
                    normalized.scopeId(),
                    normalized.startDateUtc(),
                    normalized.endDateUtc(),
                    normalized.transactionType(),
                    normalized.pageable());
        } else {
            historyPage = transactionRepository.findCustomerHistory(
                    normalized.scopeId(),
                    normalized.startDateUtc(),
                    normalized.endDateUtc(),
                    normalized.transactionType(),
                    normalized.pageable());
        }

        return historyResponseMapper.toSchema(historyPage);
    }
}
