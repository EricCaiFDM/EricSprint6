import { apiClient, getApiErrorDetails } from "./api";
import { resolveCurrentCustomerProfile } from "./customers";

export type BankAccount = {
  accountId: string;
  accountName: string;
  accountType: "Everyday" | "Savings" | "Credit";
  accountNumberMasked: string;
  checkingNumber: number | null;
  interestRate: number;
  availableBalance: number;
  currentBalance: number;
  currency: string;
  status: "Active" | "Paused";
};

export type CreateCustomerAccountInput = {
  customerId?: string;
  accountType: "CHECKING" | "SAVINGS";
  currencyCode: string;
  nickname?: string;
  interestRate?: number;
};

export type UpdateCustomerAccountInput = {
  accountId: string;
  nickname?: string;
  status?: "ACTIVE" | "SUSPENDED" | "CLOSED";
};

export type DeleteCustomerAccountResult = {
  status: "DELETED" | "CLOSED";
  message: string;
};

export async function fetchAccounts(customerId?: string): Promise<BankAccount[]> {
  try {
    const resolvedCustomerId = await resolveCustomerIdForAccounts(customerId);
    const response = await apiClient.get("/accounts", {
      params: {
        customerId: resolvedCustomerId,
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
    const customerId = await resolveCustomerIdForAccounts(input.customerId);
    const response = await apiClient.post("/accounts", {
      customerId,
      accountType: input.accountType,
      currencyCode: input.currencyCode.trim().toUpperCase(),
      nickname: input.nickname?.trim() || undefined,
      interestRate: input.accountType === "SAVINGS" && typeof input.interestRate === "number"
        ? input.interestRate
        : undefined
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

export async function fetchAccountDetails(accountId: string): Promise<BankAccount> {
  if (!accountId || accountId.trim().length === 0) {
    throw new Error("Enter a valid account ID.");
  }

  try {
    const response = await apiClient.get(`/accounts/${encodeURIComponent(accountId.trim())}`);
    const mapped = mapAccount(response.data);
    if (!mapped) {
      throw new Error("Account payload is invalid");
    }
    return mapped;
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to access the selected account.");
    }
    if (details.status === 404) {
      throw new Error("The selected account could not be found.");
    }
    throw new Error(details.message);
  }
}

export async function updateCustomerAccount(input: UpdateCustomerAccountInput): Promise<BankAccount> {
  if (!input.accountId || input.accountId.trim().length === 0) {
    throw new Error("Enter a valid account ID.");
  }

  if (!input.nickname && !input.status) {
    throw new Error("Provide at least one account field to update.");
  }

  try {
    const response = await apiClient.patch(`/accounts/${encodeURIComponent(input.accountId.trim())}`, {
      nickname: input.nickname?.trim() || undefined,
      status: input.status
    });

    const mapped = mapAccount(response.data);
    if (!mapped) {
      throw new Error("Account payload is invalid");
    }
    return mapped;
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to update the selected account.");
    }
    if (details.status === 404) {
      throw new Error("The selected account could not be found.");
    }
    throw new Error(details.message);
  }
}

export async function deleteCustomerAccount(accountId: string): Promise<DeleteCustomerAccountResult> {
  if (!accountId || accountId.trim().length === 0) {
    throw new Error("Enter a valid account ID.");
  }

  try {
    const response = await apiClient.delete(`/accounts/${encodeURIComponent(accountId.trim())}`);
    const payload = response.data as Record<string, unknown>;
    const status = asString(payload?.status, "DELETED");
    return {
      status: status === "CLOSED" ? "CLOSED" : "DELETED",
      message: asString(payload?.message, "Account deleted")
    };
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to delete the selected account.");
    }
    if (details.status === 404) {
      throw new Error("The selected account could not be found.");
    }
    if (details.status === 409) {
      throw new Error("Account deletion is blocked by policy or linked dependencies.");
    }
    throw new Error(details.message);
  }
}

async function resolveCustomerIdForAccounts(customerId?: string): Promise<string> {
  if (customerId && customerId.trim().length > 0) {
    return customerId.trim();
  }

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
    checkingNumber: asNullableNumber(data.checkingNumber),
    interestRate: asNumber(data.interestRate, 0),
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

function asNullableNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
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
