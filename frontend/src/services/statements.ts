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

export async function fetchStatements(): Promise<StatementItem[]> {
  const response = await apiClient.get("/statements");
  return mapStatements(response.data);
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
