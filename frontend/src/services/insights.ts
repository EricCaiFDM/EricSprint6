import { apiClient } from "./api";

export type InsightCategory = {
  category: string;
  amount: number;
  ratio: number;
  trend: "up" | "down" | "flat";
};

export type SpendingInsight = {
  periodLabel: string;
  totalSpend: number;
  currency: string;
  confidenceLabel: "High confidence" | "Medium confidence";
  categories: InsightCategory[];
};

export async function fetchSpendingInsights(): Promise<SpendingInsight> {
  const response = await apiClient.get("/insights/spending");
  return mapInsights(response.data);
}

function mapInsights(payload: unknown): SpendingInsight {
  if (!payload || typeof payload !== "object") {
    throw new Error("Spending insights payload is invalid");
  }

  const data = payload as Record<string, unknown>;
  const categories = mapCategories(data.categories);

  return {
    periodLabel: asString(data.periodLabel, "Current period"),
    totalSpend: asNumber(data.totalSpend, 0),
    currency: asString(data.currency, "USD"),
    confidenceLabel:
      asString(data.confidenceLabel, "High confidence") === "Medium confidence"
        ? "Medium confidence"
        : "High confidence",
    categories
  };
}

function mapCategories(payload: unknown): InsightCategory[] {
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      return {
        category: asString(data.category, "General"),
        amount: asNumber(data.amount, 0),
        ratio: asNumber(data.ratio, 0),
        trend: asTrend(data.trend)
      } satisfies InsightCategory;
    })
    .filter((category): category is InsightCategory => category !== null);
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

function asTrend(value: unknown): InsightCategory["trend"] {
  if (value === "up" || value === "down" || value === "flat") {
    return value;
  }
  return "flat";
}
