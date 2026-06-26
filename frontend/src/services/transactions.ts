import { apiClient, getApiErrorDetails } from "./api";
import { resolveCurrentCustomerProfile } from "./customers";

export type TransactionItem = {
  transactionId: string;
  bookedAt: string;
  description: string;
  category: string;
  amount: number;
  currency: string;
  direction: "DEBIT" | "CREDIT";
  status: "Completed" | "Pending";
};

export type TransferInput = {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  note?: string;
};

export type TransferReceipt = {
  reference: string;
  status: "Completed";
  submittedAt: string;
};

export async function fetchRecentTransactions(): Promise<TransactionItem[]> {
  const customer = await resolveCurrentCustomerProfile();

  try {
    const response = await apiClient.get("/transactions/history", {
      params: {
        scopeType: "CUSTOMER",
        scopeId: customer.customerId,
        page: 1,
        pageSize: 8
      }
    });

    return mapTransactions(response.data);
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error(
        "This signed-in account is not authorized to view transaction history for the selected customer."
      );
    }

    if (details.code === "TRANSACTION_SCOPE_NOT_FOUND" || details.status === 404) {
      throw new Error("No transaction scope was found for this sign-in. Ensure customer and accounts are set up first.");
    }

    throw new Error(details.message);
  }
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

  const response = await apiClient.post("/transactions/transfer", {
    sourceAccountId: input.sourceAccountId,
    destinationAccountId: input.destinationAccountId,
    amount: amount.toFixed(2)
  }, {
    headers: {
      "Idempotency-Key": idempotencyKey
    }
  });

  return {
    reference: asString(response.data?.transferId, `NB-${Date.now()}`),
    status: "Completed",
    submittedAt: asString(response.data?.postedAtUtc, new Date().toISOString())
  };
}

function mapTransactions(payload: unknown): TransactionItem[] {
  if (!payload || typeof payload !== "object") {
    return [];
  }

  const data = payload as { items?: unknown };
  if (!Array.isArray(data.items)) {
    return [];
  }

  return data.items
    .map((row): TransactionItem | null => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      const transactionId = asString(data.transactionId, "");
      if (!transactionId) {
        return null;
      }

      const amount = asNumber(data.amount, 0);
      const transactionType = asString(data.transactionType, "DEPOSIT");
      const direction: TransactionItem["direction"] =
        transactionType === "DEPOSIT" || transactionType === "TRANSFER_CREDIT" ? "CREDIT" : "DEBIT";

      return {
        transactionId,
        bookedAt: asString(data.postedAtUtc, new Date().toISOString()),
        description: asString(data.description, mapDescription(transactionType)),
        category: asString(data.category, mapCategory(transactionType)),
        amount,
        currency: asString(data.currencyCode, "USD"),
        direction,
        status: "Completed" as TransactionItem["status"]
      } satisfies TransactionItem;
    })
    .filter((transaction): transaction is TransactionItem => transaction !== null);
}

function mapDescription(transactionType: string): string {
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

function mapCategory(transactionType: string): string {
  if (transactionType.startsWith("TRANSFER")) {
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
