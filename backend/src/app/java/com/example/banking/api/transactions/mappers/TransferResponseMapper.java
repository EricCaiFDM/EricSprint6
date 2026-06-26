package com.example.banking.api.transactions.mappers;

import org.springframework.stereotype.Component;

import com.example.banking.api.transactions.schemas.TransferResponseSchema;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransferLinkEntity;

@Component
public class TransferResponseMapper {
    public TransferResponseSchema toSchema(
            TransferLinkEntity transferLink,
            TransactionEntity debitTransaction,
            TransactionEntity creditTransaction) {
        return new TransferResponseSchema(
                transferLink.getTransferId(),
                debitTransaction.getTransactionId(),
                creditTransaction.getTransactionId(),
                debitTransaction.getAmount().toPlainString(),
                debitTransaction.getCurrencyCode(),
                debitTransaction.getBalanceAfter().toPlainString(),
                creditTransaction.getBalanceAfter().toPlainString(),
                debitTransaction.getPostedAtUtc().toString());
    }
}
