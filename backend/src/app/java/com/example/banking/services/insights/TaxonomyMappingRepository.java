package com.example.banking.services.insights;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

@Component
public class TaxonomyMappingRepository {
    private static final Map<TransactionType, TaxonomyMapping> DEFAULT_MAPPINGS = Map.of(
            TransactionType.WITHDRAWAL,
            new TaxonomyMapping("CASH_WITHDRAWAL", "Cash withdrawals"),
            TransactionType.TRANSFER_DEBIT,
            new TaxonomyMapping("TRANSFER_OUT", "Transfers out"),
            TransactionType.DEPOSIT,
            new TaxonomyMapping("DEPOSIT_INCOME", "Deposits"),
            TransactionType.TRANSFER_CREDIT,
            new TaxonomyMapping("TRANSFER_IN", "Transfers in"));

    public TaxonomyMapping resolve(TransactionEntity transaction) {
        if (transaction == null || transaction.getTransactionType() == null) {
            return null;
        }
        return DEFAULT_MAPPINGS.get(transaction.getTransactionType());
    }

    public Set<String> supportedCategoryCodes() {
        return DEFAULT_MAPPINGS.values().stream().map(TaxonomyMapping::categoryCode).collect(java.util.stream.Collectors.toSet());
    }

    public record TaxonomyMapping(String categoryCode, String categoryLabel) {
    }
}
