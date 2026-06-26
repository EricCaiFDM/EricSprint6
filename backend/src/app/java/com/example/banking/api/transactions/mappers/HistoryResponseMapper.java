package com.example.banking.api.transactions.mappers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.example.banking.api.transactions.schemas.TransactionHistoryItemSchema;
import com.example.banking.api.transactions.schemas.TransactionHistoryResponseSchema;
import com.example.banking.models.TransactionEntity;

@Component
public class HistoryResponseMapper {
    public TransactionHistoryResponseSchema toSchema(Page<TransactionEntity> page) {
        List<TransactionHistoryItemSchema> items = page.getContent().stream()
                .map(this::toItem)
                .toList();

        int totalPages = Math.max(1, page.getTotalPages());
        return new TransactionHistoryResponseSchema(
                items,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                totalPages);
    }

    private TransactionHistoryItemSchema toItem(TransactionEntity transaction) {
        return new TransactionHistoryItemSchema(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getTransactionType().name(),
                transaction.getAmount().toPlainString(),
                transaction.getCurrencyCode(),
                transaction.getPostedAtUtc().toString(),
                transaction.getBalanceAfter().toPlainString(),
                transaction.getCorrelationId());
    }
}
