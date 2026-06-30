package com.example.banking.services.insights;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

@Service
public class InsightDataVisibilityService {
    public List<TransactionEntity> filterVisibleSpendingTransactions(List<TransactionEntity> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL
                        || transaction.getTransactionType() == TransactionType.TRANSFER_DEBIT)
                .toList();
    }
}
