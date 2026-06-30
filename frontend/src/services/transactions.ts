import { apiClient, getApiErrorDetails } from "./api";
import { resolveCurrentCustomerProfile } from "./customers";

export type TransactionType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER_DEBIT" | "TRANSFER_CREDIT";
export type TransactionTypeFilter = "ALL" | TransactionType;
export type HistoryScopeType = "CUSTOMER" | "ACCOUNT";

export type TransactionItem = {
  transactionId: string;
  accountId?: string;
  transactionType: TransactionType;
  bookedAt: string;
  balanceAfter?: number;
  description: string;
  category: string;
  amount: number;
  currency: string;
  direction: "DEBIT" | "CREDIT";
  status: "Completed" | "Pending";
};

export type TransactionHistoryQuery = {
  scopeType?: HistoryScopeType;
  scopeId?: string;
  customerId?: string;
  transactionType?: TransactionTypeFilter;
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
};

export type TransactionHistoryResult = {
  items: TransactionItem[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
};

export type PostingInput = {
  accountId: string;
  amount: number;
  customerId?: string;
};

export type PostingReceipt = {
  reference: string;
  transactionType: "DEPOSIT" | "WITHDRAWAL";
  status: "Completed";
  submittedAt: string;
  postedAmount: number;
  currency: string;
  balanceAfter: number;
};

export type TransferInput = {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  customerId?: string;
  note?: string;
};

export type TransferReceipt = {
  reference: string;
  status: "Completed";
  submittedAt: string;
  postedAmount: number;
  currency: string;
  sourceBalanceAfter: number;
  destinationBalanceAfter: number;
};

export async function fetchRecentTransactions(): Promise<TransactionItem[]> {
  const history = await fetchTransactionHistory({
    scopeType: "CUSTOMER",
    page: 1,
    pageSize: 8
  });

  return history.items;
}

export async function fetchTransactionHistory(
  query: TransactionHistoryQuery = {}
): Promise<TransactionHistoryResult> {
  const scopeType = query.scopeType ?? "CUSTOMER";
  const scopedCustomerId = query.customerId?.trim();
  const scopedQueryScopeId = query.scopeId?.trim();

  let scopeId = "";

  if (scopeType === "CUSTOMER") {
    if (scopedQueryScopeId) {
      scopeId = scopedQueryScopeId;
    } else if (scopedCustomerId) {
      scopeId = scopedCustomerId;
    } else {
      const customer = await resolveCurrentCustomerProfile();
      scopeId = customer.customerId;
    }
  } else {
    scopeId = scopedQueryScopeId ?? "";
  }

  if (!scopeId) {
    throw new Error("Select a valid scope before requesting transaction history.");
  }

  const page = Math.max(1, Math.trunc(query.page ?? 1));
  const pageSize = Math.max(1, Math.min(Math.trunc(query.pageSize ?? 20), 100));

  const startDateUtc = toStartOfDayUtc(query.startDate);
  const endDateUtc = toEndOfDayUtc(query.endDate);
  if (startDateUtc && endDateUtc && Date.parse(startDateUtc) > Date.parse(endDateUtc)) {
    throw new Error("Start date cannot be after end date.");
  }

  const params: Record<string, string | number> = {
    scopeType,
    scopeId,
    page,
    pageSize
  };

  if (query.transactionType && query.transactionType !== "ALL") {
    params.transactionType = query.transactionType;
  }

  if (startDateUtc) {
    params.startDateUtc = startDateUtc;
  }

  if (endDateUtc) {
    params.endDateUtc = endDateUtc;
  }

  try {
    const response = await apiClient.get("/transactions/history", { params });
    return mapHistoryResponse(response.data, page, pageSize);
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to view transaction history for the selected scope.");
    }

    if (details.code === "TRANSACTION_SCOPE_NOT_FOUND" || details.status === 404) {
      throw new Error("No transaction scope was found. Ensure customer and accounts are set up first.");
    }

    throw new Error(details.message);
  }
}

export async function submitDeposit(input: PostingInput): Promise<PostingReceipt> {
  return submitPosting("deposit", input, "DEPOSIT");
}

export async function submitWithdrawal(input: PostingInput): Promise<PostingReceipt> {
  return submitPosting("withdrawal", input, "WITHDRAWAL");
}

export async function submitTransfer(input: TransferInput): Promise<TransferReceipt> {
  const amount = Number(input.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    throw new Error("Enter a valid transfer amount greater than zero.");
  }

  if (!input.sourceAccountId || !input.destinationAccountId) {
    throw new Error("Select both source and destination accounts.");
  }

  if (input.sourceAccountId === input.destinationAccountId) {
    throw new Error("Source and destination accounts must be different.");
  }

  const idempotencyKey = buildIdempotencyKey("transfer");

  try {
    const response = await apiClient.post(
      "/transactions/transfer",
      {
        sourceAccountId: input.sourceAccountId,
        destinationAccountId: input.destinationAccountId,
        amount: amount.toFixed(2),
        customerId: input.customerId?.trim() || undefined
      },
      {
        headers: {
          "Idempotency-Key": idempotencyKey
        }
      }
    );

    return {
      reference: asString(response.data?.transferId, `NB-${Date.now()}`),
      status: "Completed",
      submittedAt: asString(response.data?.postedAtUtc, new Date().toISOString()),
      postedAmount: asNumber(response.data?.postedAmount, amount),
      currency: asString(response.data?.currencyCode, "USD"),
      sourceBalanceAfter: asNumber(response.data?.sourceBalanceAfter, 0),
      destinationBalanceAfter: asNumber(response.data?.destinationBalanceAfter, 0)
    };
  } catch (error) {
    throw mapOperationError("transfer", error);
  }
}

async function submitPosting(
  operation: "deposit" | "withdrawal",
  input: PostingInput,
  expectedType: "DEPOSIT" | "WITHDRAWAL"
): Promise<PostingReceipt> {
  const amount = Number(input.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    throw new Error("Enter a valid amount greater than zero.");
  }

  if (!input.accountId) {
    throw new Error("Select an account before submitting.");
  }

  const idempotencyKey = buildIdempotencyKey(operation);

  try {
    const response = await apiClient.post(
      `/transactions/${operation}`,
      {
        accountId: input.accountId,
        amount: amount.toFixed(2),
        customerId: input.customerId?.trim() || undefined
      },
      {
        headers: {
          "Idempotency-Key": idempotencyKey
        }
      }
    );

    return {
      reference: asString(response.data?.transactionId, `NB-${Date.now()}`),
      transactionType: expectedType,
      status: "Completed",
      submittedAt: asString(response.data?.postedAtUtc, new Date().toISOString()),
      postedAmount: asNumber(response.data?.postedAmount, amount),
      currency: asString(response.data?.currencyCode, "USD"),
      balanceAfter: asNumber(response.data?.balanceAfter, 0)
    };
  } catch (error) {
    throw mapOperationError(operation, error);
  }
}

function mapHistoryResponse(payload: unknown, fallbackPage: number, fallbackPageSize: number): TransactionHistoryResult {
  if (!payload || typeof payload !== "object") {
    return {
      items: [],
      page: fallbackPage,
      pageSize: fallbackPageSize,
      totalItems: 0,
      totalPages: 1
    };
  }

  const row = payload as Record<string, unknown>;
  return {
    items: mapTransactions(payload),
    page: Math.max(1, asNumber(row.page, fallbackPage)),
    pageSize: Math.max(1, asNumber(row.pageSize, fallbackPageSize)),
    totalItems: Math.max(0, asNumber(row.totalItems, 0)),
    totalPages: Math.max(1, asNumber(row.totalPages, 1))
  };
}

function mapTransactions(payload: unknown): TransactionItem[] {
  if (!payload || typeof payload !== "object") {
    return [];
  }

  const row = payload as { items?: unknown };
  if (!Array.isArray(row.items)) {
    return [];
  }

  return row.items
    .map((item): TransactionItem | null => {
      if (!item || typeof item !== "object") {
        return null;
      }

      const data = item as Record<string, unknown>;
      const transactionId = asString(data.transactionId, "");
      if (!transactionId) {
        return null;
      }

      const transactionType = asTransactionType(data.transactionType);
      const amount = asNumber(data.amount, 0);
      const balanceAfter = asOptionalNumber(data.balanceAfter);
      const direction: TransactionItem["direction"] =
        transactionType === "DEPOSIT" || transactionType === "TRANSFER_CREDIT" ? "CREDIT" : "DEBIT";

      return {
        transactionId,
        accountId: asString(data.accountId, "") || undefined,
        transactionType,
        bookedAt: asString(data.postedAtUtc, new Date().toISOString()),
        ...(balanceAfter === undefined ? {} : { balanceAfter }),
        description: asString(data.description, mapDescription(transactionType)),
        category: asString(data.category, mapCategory(transactionType)),
        amount,
        currency: asString(data.currencyCode, "USD"),
        direction,
        status: "Completed"
      };
    })
    .filter((transaction): transaction is TransactionItem => transaction !== null);
}

function asTransactionType(value: unknown): TransactionType {
  const type = asString(value, "DEPOSIT");
  if (type === "WITHDRAWAL" || type === "TRANSFER_DEBIT" || type === "TRANSFER_CREDIT") {
    return type;
  }
  return "DEPOSIT";
}

function mapDescription(transactionType: TransactionType): string {
  switch (transactionType) {
    case "DEPOSIT":
      return "Deposit";
    case "WITHDRAWAL":
      return "Withdrawal";
    case "TRANSFER_DEBIT":
      return "Transfer sent";
    case "TRANSFER_CREDIT":
      return "Transfer received";
    default:
      return "Transaction";
  }
}

function mapCategory(transactionType: TransactionType): string {
  if (transactionType === "TRANSFER_DEBIT" || transactionType === "TRANSFER_CREDIT") {
    return "Transfer";
  }
  if (transactionType === "DEPOSIT") {
    return "Deposit";
  }
  if (transactionType === "WITHDRAWAL") {
    return "Withdrawal";
  }
  return "General";
}

function mapOperationError(
  operation: "deposit" | "withdrawal" | "transfer",
  error: unknown
): Error {
  const details = getApiErrorDetails(error);

  if (details.status === 422 || details.code === "TRANSACTION_INSUFFICIENT_FUNDS") {
    if (operation === "withdrawal" || operation === "transfer") {
      return new Error("Insufficient funds for this operation.");
    }
  }

  if (details.status === 403) {
    return new Error("This signed-in account is not authorized to perform this transaction.");
  }

  if (details.status === 404 || details.code === "TRANSACTION_ACCOUNT_NOT_FOUND") {
    return new Error("The target account or transaction scope could not be found.");
  }

  if (details.status === 409 || details.code === "TRANSACTION_IDEMPOTENCY_CONFLICT") {
    return new Error("This transaction conflicted with a prior request. Please retry.");
  }

  return new Error(details.message || `Unable to complete ${operation}.`);
}

function toStartOfDayUtc(value?: string): string | undefined {
  if (!value || !value.trim()) {
    return undefined;
  }
  return `${value.trim()}T00:00:00.000Z`;
}

function toEndOfDayUtc(value?: string): string | undefined {
  if (!value || !value.trim()) {
    return undefined;
  }
  return `${value.trim()}T23:59:59.999Z`;
}

function buildIdempotencyKey(operation: string): string {
  const random = Math.random().toString(36).slice(2, 12);
  return `${operation}-${Date.now()}-${random}`;
}

function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asNumber(value: unknown, fallback: number): number {
  if (typeof value === "number") {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
  return fallback;
}

function asOptionalNumber(value: unknown): number | undefined {
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : undefined;
  }

  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  return undefined;
}
