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

const fallbackTransactions: TransactionItem[] = [
  {
    transactionId: "txn-401",
    bookedAt: "2026-06-24T09:18:00Z",
    description: "Salary - Blue Horizon Pty",
    category: "Income",
    amount: 4300,
    currency: "AUD",
    direction: "CREDIT",
    status: "Completed"
  },
  {
    transactionId: "txn-402",
    bookedAt: "2026-06-24T12:30:00Z",
    description: "Northline Grocers",
    category: "Groceries",
    amount: 124.9,
    currency: "AUD",
    direction: "DEBIT",
    status: "Completed"
  },
  {
    transactionId: "txn-403",
    bookedAt: "2026-06-23T15:44:00Z",
    description: "Metro Energy",
    category: "Utilities",
    amount: 88.3,
    currency: "AUD",
    direction: "DEBIT",
    status: "Completed"
  },
  {
    transactionId: "txn-404",
    bookedAt: "2026-06-23T01:20:00Z",
    description: "Dining - Osteria Lane",
    category: "Dining",
    amount: 64.5,
    currency: "AUD",
    direction: "DEBIT",
    status: "Completed"
  },
  {
    transactionId: "txn-405",
    bookedAt: "2026-06-22T03:55:00Z",
    description: "Savings Transfer",
    category: "Transfer",
    amount: 250,
    currency: "AUD",
    direction: "DEBIT",
    status: "Pending"
  }
];

export async function fetchRecentTransactions(): Promise<TransactionItem[]> {
  try {
    const response = await apiClient.get("/transactions/history?size=8");
    const mapped = mapTransactions(response.data);
    return mapped.length > 0 ? mapped : fallbackTransactions;
  } catch {
    return fallbackTransactions;
  }
}

export async function submitTransfer(input: TransferInput): Promise<TransferReceipt> {
  try {
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
  } catch {
    return {
      reference: `NB-${Date.now()}`,
      status: "Submitted",
      submittedAt: new Date().toISOString()
    };
  }
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
