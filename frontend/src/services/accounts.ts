import { apiClient } from "./api";

export type BankAccount = {
  accountId: string;
  accountName: string;
  accountType: "Everyday" | "Savings" | "Credit";
  accountNumberMasked: string;
  availableBalance: number;
  currentBalance: number;
  currency: string;
  status: "Active" | "Paused";
};

const fallbackAccounts: BankAccount[] = [
  {
    accountId: "acc-main",
    accountName: "Everyday Banking",
    accountType: "Everyday",
    accountNumberMasked: "**** 1894",
    availableBalance: 6234.52,
    currentBalance: 6234.52,
    currency: "AUD",
    status: "Active"
  },
  {
    accountId: "acc-save",
    accountName: "Rainy Day Saver",
    accountType: "Savings",
    accountNumberMasked: "**** 0216",
    availableBalance: 15420.11,
    currentBalance: 15420.11,
    currency: "AUD",
    status: "Active"
  },
  {
    accountId: "acc-credit",
    accountName: "Rewards Credit",
    accountType: "Credit",
    accountNumberMasked: "**** 9923",
    availableBalance: 2410.8,
    currentBalance: -589.2,
    currency: "AUD",
    status: "Active"
  }
];

export async function fetchAccounts(): Promise<BankAccount[]> {
  try {
    const response = await apiClient.get("/accounts");
    const mapped = mapAccounts(response.data);
    return mapped.length > 0 ? mapped : fallbackAccounts;
  } catch {
    return fallbackAccounts;
  }
}

function mapAccounts(payload: unknown): BankAccount[] {
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      const accountId = asString(data.accountId, "");
      if (!accountId) {
        return null;
      }

      const accountType = asType(data.accountType);
      return {
        accountId,
        accountName: asString(data.accountName, `${accountType} Account`),
        accountType,
        accountNumberMasked: asString(data.accountNumberMasked, `**** ${accountId.slice(-4)}`),
        availableBalance: asNumber(data.availableBalance, asNumber(data.balance, 0)),
        currentBalance: asNumber(data.currentBalance, asNumber(data.balance, 0)),
        currency: asString(data.currency, "AUD"),
        status: asString(data.status, "Active") === "Paused" ? "Paused" : "Active"
      } satisfies BankAccount;
    })
    .filter((account): account is BankAccount => account !== null);
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

function asType(value: unknown): BankAccount["accountType"] {
  if (value === "Everyday" || value === "Savings" || value === "Credit") {
    return value;
  }
  if (value === "CHECKING") {
    return "Everyday";
  }
  return "Savings";
}
