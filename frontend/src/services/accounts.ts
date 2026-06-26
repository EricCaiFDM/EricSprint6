import { apiClient, getApiErrorDetails } from "./api";
import { resolveCurrentCustomerProfile } from "./customers";

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

export type CreateCustomerAccountInput = {
  accountType: "CHECKING" | "SAVINGS";
  currencyCode: string;
  nickname?: string;
};

export async function fetchAccounts(): Promise<BankAccount[]> {
  try {
    const customerId = await resolveCustomerIdForAccounts();
    const response = await apiClient.get("/accounts", {
      params: {
        customerId,
        page: 1,
        pageSize: 20
      }
    });
    return mapAccounts(response.data);
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error(
        "This signed-in account is not authorized for the selected customer accounts. " +
          "Sign out and sign in with the correct account, then retry."
      );
    }
    throw new Error(details.message);
  }
}

export async function createCustomerAccount(input: CreateCustomerAccountInput): Promise<BankAccount> {
  try {
    const customerId = await resolveCustomerIdForAccounts();
    const response = await apiClient.post("/accounts", {
      customerId,
      accountType: input.accountType,
      currencyCode: input.currencyCode.trim().toUpperCase(),
      nickname: input.nickname?.trim() || undefined
    });

    const mapped = mapAccount(response.data);
    if (!mapped) {
      throw new Error("Account was created but response payload was invalid");
    }

    return mapped;
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.code === "CUSTOMER_NOT_FOUND") {
      throw new Error("No customer account record found for this sign-in. Complete account setup first.");
    }
    if (details.status === 404) {
      throw new Error("Account service could not find the requested resource. Verify customer profile setup and try again.");
    }
    if (details.status === 403) {
      throw new Error(
        "This signed-in account is not authorized to open accounts for the selected customer. " +
          "Sign out and sign in with the correct account, then retry."
      );
    }
    throw new Error(details.message);
  }
}

async function resolveCustomerIdForAccounts(): Promise<string> {
  const profile = await resolveCurrentCustomerProfile();
  return profile.customerId;
}

function mapAccounts(payload: unknown): BankAccount[] {
  const rows = Array.isArray(payload)
    ? payload
    : payload && typeof payload === "object" && Array.isArray((payload as { items?: unknown }).items)
      ? ((payload as { items: unknown[] }).items ?? [])
      : [];

  if (!Array.isArray(rows)) {
    return [];
  }

  return rows
    .map((row) => mapAccount(row))
    .filter((account): account is BankAccount => account !== null);
}

function mapAccount(payload: unknown): BankAccount | null {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  const data = payload as Record<string, unknown>;
  const accountId = asString(data.accountId, "");
  if (!accountId) {
    return null;
  }

  const accountType = asType(data.accountType);
  const accountNumber = asString(data.accountNumber, accountId);
  const balance = asNumber(data.balance, 0);
  return {
    accountId,
    accountName: asString(data.nickname, asString(data.accountName, `${accountType} Account`)),
    accountType,
    accountNumberMasked: asString(data.accountNumberMasked, maskAccountNumber(accountNumber)),
    availableBalance: asNumber(data.availableBalance, balance),
    currentBalance: asNumber(data.currentBalance, balance),
    currency: asString(data.currencyCode, asString(data.currency, "USD")),
    status: asStatus(data.status)
  } satisfies BankAccount;
}

function maskAccountNumber(value: string): string {
  const visible = value.slice(-4);
  return `**** ${visible}`;
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
  if (value === "SAVINGS") {
    return "Savings";
  }
  return "Everyday";
}

function asStatus(value: unknown): BankAccount["status"] {
  if (value === "ACTIVE") {
    return "Active";
  }
  if (value === "SUSPENDED" || value === "CLOSED") {
    return "Paused";
  }
  return value === "Paused" ? "Paused" : "Active";
}
