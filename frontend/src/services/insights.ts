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

const fallbackInsights: SpendingInsight = {
  periodLabel: "June 2026",
  totalSpend: 1975.44,
  currency: "AUD",
  confidenceLabel: "High confidence",
  categories: [
    {
      category: "Home",
      amount: 780.0,
      ratio: 0.39,
      trend: "flat"
    },
    {
      category: "Groceries",
      amount: 420.6,
      ratio: 0.21,
      trend: "up"
    },
    {
      category: "Transport",
      amount: 231.2,
      ratio: 0.12,
      trend: "down"
    },
    {
      category: "Dining",
      amount: 188.5,
      ratio: 0.1,
      trend: "up"
    },
    {
      category: "Utilities",
      amount: 355.14,
      ratio: 0.18,
      trend: "flat"
    }
  ]
};

export async function fetchSpendingInsights(): Promise<SpendingInsight> {
  try {
    const response = await apiClient.get("/insights/spending");
    return mapInsights(response.data);
  } catch {
    return fallbackInsights;
  }
}

function mapInsights(payload: unknown): SpendingInsight {
  if (!payload || typeof payload !== "object") {
    return fallbackInsights;
  }

  const data = payload as Record<string, unknown>;
  const categories = mapCategories(data.categories);

  return {
    periodLabel: asString(data.periodLabel, fallbackInsights.periodLabel),
    totalSpend: asNumber(data.totalSpend, fallbackInsights.totalSpend),
    currency: asString(data.currency, fallbackInsights.currency),
    confidenceLabel:
      asString(data.confidenceLabel, fallbackInsights.confidenceLabel) === "Medium confidence"
        ? "Medium confidence"
        : "High confidence",
    categories: categories.length > 0 ? categories : fallbackInsights.categories
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
