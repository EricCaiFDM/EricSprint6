import { apiClient } from "./api";

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
  recipientName: string;
  destinationAccountId: string;
  amount: number;
  note: string;
};

export type TransferReceipt = {
  reference: string;
  status: "Submitted" | "Completed";
  submittedAt: string;
};

export async function fetchRecentTransactions(): Promise<TransactionItem[]> {
  const response = await apiClient.get("/transactions/history?size=8");
  return mapTransactions(response.data);
}

export async function submitTransfer(input: TransferInput): Promise<TransferReceipt> {
  const response = await apiClient.post("/transactions/transfer", {
    sourceAccountId: input.sourceAccountId,
    destinationAccountId: input.destinationAccountId,
    amount: input.amount,
    currency: "AUD",
    note: input.note,
    recipientName: input.recipientName
  });

  return {
    reference: asString(response.data?.reference, `NB-${Date.now()}`),
    status: "Submitted",
    submittedAt: new Date().toISOString()
  };
}

function mapTransactions(payload: unknown): TransactionItem[] {
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      const transactionId = asString(data.transactionId, "");
      if (!transactionId) {
        return null;
      }

      const amount = asNumber(data.amount, 0);
      const direction: TransactionItem["direction"] = amount < 0 ? "DEBIT" : "CREDIT";

      return {
        transactionId,
        bookedAt: asString(data.bookedAt, new Date().toISOString()),
        description: asString(data.description, "Card purchase"),
        category: asString(data.category, "General"),
        amount: Math.abs(amount),
        currency: asString(data.currency, "AUD"),
        direction,
        status: asString(data.status, "Completed") === "Pending" ? "Pending" : "Completed"
      } satisfies TransactionItem;
    })
    .filter((transaction): transaction is TransactionItem => transaction !== null);
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
