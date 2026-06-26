import { apiClient } from "./api";

export type StatementItem = {
  statementId: string;
  periodLabel: string;
  accountName: string;
  issuedAt: string;
  closingBalance: number;
  currency: string;
  status: "Ready" | "Processing";
};

const fallbackStatements: StatementItem[] = [
  {
    statementId: "stmt-2026-05",
    periodLabel: "May 2026",
    accountName: "Everyday Banking",
    issuedAt: "2026-06-01T02:10:00Z",
    closingBalance: 5988.32,
    currency: "AUD",
    status: "Ready"
  },
  {
    statementId: "stmt-2026-04",
    periodLabel: "Apr 2026",
    accountName: "Everyday Banking",
    issuedAt: "2026-05-01T02:12:00Z",
    closingBalance: 5642.08,
    currency: "AUD",
    status: "Ready"
  },
  {
    statementId: "stmt-2026-06",
    periodLabel: "Jun 2026",
    accountName: "Everyday Banking",
    issuedAt: "2026-07-01T02:12:00Z",
    closingBalance: 0,
    currency: "AUD",
    status: "Processing"
  }
];

export async function fetchStatements(): Promise<StatementItem[]> {
  try {
    const response = await apiClient.get("/statements");
    const mapped = mapStatements(response.data);
    return mapped.length > 0 ? mapped : fallbackStatements;
  } catch {
    return fallbackStatements;
  }
}

function mapStatements(payload: unknown): StatementItem[] {
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }

      const data = row as Record<string, unknown>;
      const statementId = asString(data.statementId, "");
      if (!statementId) {
        return null;
      }

      return {
        statementId,
        periodLabel: asString(data.periodLabel, "Statement"),
        accountName: asString(data.accountName, "Primary Account"),
        issuedAt: asString(data.issuedAt, new Date().toISOString()),
        closingBalance: asNumber(data.closingBalance, 0),
        currency: asString(data.currency, "AUD"),
        status: asString(data.status, "Ready") === "Processing" ? "Processing" : "Ready"
      } satisfies StatementItem;
    })
    .filter((statement): statement is StatementItem => statement !== null);
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
