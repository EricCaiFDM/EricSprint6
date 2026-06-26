package com.example.banking.lib.observability;

public final class TransactionLogFields {
    public static final String OPERATION = "operation";
    public static final String ACCOUNT_ID = "accountId";
    public static final String SOURCE_ACCOUNT_ID = "sourceAccountId";
    public static final String DESTINATION_ACCOUNT_ID = "destinationAccountId";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String TRANSFER_ID = "transferId";
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";
    public static final String ACTOR_USER_ID = "actorUserId";
    public static final String ACTOR_ROLE = "actorRole";
    public static final String OUTCOME = "outcome";
    public static final String REASON_CODE = "reasonCode";

    private TransactionLogFields() {
    }
}
