package com.example.banking.services;

import java.math.BigDecimal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.lib.TransferLinkJpaRepository;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.models.TransferLinkEntity;

@Service
public class TransferLinkService {
    private final TransferLinkJpaRepository transferLinkJpaRepository;
    private final MoneyPolicy moneyPolicy;

    public TransferLinkService(TransferLinkJpaRepository transferLinkJpaRepository, MoneyPolicy moneyPolicy) {
        this.transferLinkJpaRepository = transferLinkJpaRepository;
        this.moneyPolicy = moneyPolicy;
    }

    @Transactional
    public TransferLinkEntity persistTransferLink(
            String debitTransactionId,
            String creditTransactionId,
            String sourceAccountId,
            String destinationAccountId,
            BigDecimal amount,
            String currencyCode) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw TransactionErrors.conflict(
                    "sourceAccountId and destinationAccountId must be different",
                    "destinationAccountId");
        }

        TransferLinkEntity entity = new TransferLinkEntity();
        entity.setDebitTransactionId(debitTransactionId);
        entity.setCreditTransactionId(creditTransactionId);
        entity.setSourceAccountId(sourceAccountId);
        entity.setDestinationAccountId(destinationAccountId);
        entity.setAmount(moneyPolicy.normalizePositiveAmount(amount, "amount"));
        entity.setCurrencyCode(moneyPolicy.normalizeCurrency(currencyCode, "currencyCode"));

        try {
            return transferLinkJpaRepository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw TransactionErrors.conflict("Transfer linkage conflict", null);
        }
    }
}
