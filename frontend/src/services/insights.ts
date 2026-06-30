import { apiClient } from "./api";

export type InsightCategory = {
  category: string;
  amount: number;
  ratio: number;
  trend: "up" | "down" | "flat";
};

export type SpendingInsight = {
  periodLabel: string;
  periodStartUtc: string;
  periodEndUtc: string;
  scopeType: "ACCOUNT" | "CUSTOMER";
  scopeId: string;
  totalSpend: number;
  currency: string;
  confidenceLabel: "High confidence" | "Medium confidence" | "Low confidence";
  confidenceLevel: "HIGH" | "MEDIUM" | "LOW";
  coverageRatio: number;
  confidenceReason: string;
  status: "GENERATED" | "PARTIAL" | "INSUFFICIENT_DATA";
  methodology: string;
  categories: InsightCategory[];
};

export type SpendingInsightQuery = {
  scopeType?: "ACCOUNT" | "CUSTOMER";
  scopeId?: string;
  periodStartUtc?: string;
  periodEndUtc?: string;
  categoryFilters?: string;
};

export async function fetchSpendingInsights(query: SpendingInsightQuery = {}): Promise<SpendingInsight> {
  const params: Record<string, string> = {};

  if (query.scopeType) {
    params.scopeType = query.scopeType;
  }
  if (query.scopeId?.trim()) {
    params.scopeId = query.scopeId.trim();
  }
  if (query.periodStartUtc?.trim()) {
    params.periodStartUtc = query.periodStartUtc.trim();
  }
  if (query.periodEndUtc?.trim()) {
    params.periodEndUtc = query.periodEndUtc.trim();
  }
  if (query.categoryFilters?.trim()) {
    params.categoryFilters = query.categoryFilters.trim();
  }

  const response = await apiClient.get("/insights/spending", {
    params: Object.keys(params).length > 0 ? params : undefined
  });
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
    periodStartUtc: asString(data.periodStartUtc, ""),
    periodEndUtc: asString(data.periodEndUtc, ""),
    scopeType: asScopeType(data.scopeType),
    scopeId: asString(data.scopeId, ""),
    totalSpend: asNumber(data.totalSpend, 0),
    currency: asString(data.currency, "USD"),
    confidenceLabel: asConfidenceLabel(data.confidenceLabel),
    confidenceLevel: asConfidenceLevel(data.confidenceLevel),
    coverageRatio: asNumber(data.coverageRatio, 0),
    confidenceReason: asString(data.confidenceReason, "Confidence metadata unavailable."),
    status: asStatus(data.status),
    methodology: asString(
      data.methodology,
      "Spending insights use posted debit transactions and approved taxonomy mappings."
    ),
    categories
  };
}

function asScopeType(value: unknown): SpendingInsight["scopeType"] {
  return value === "ACCOUNT" ? "ACCOUNT" : "CUSTOMER";
}

function asConfidenceLabel(value: unknown): SpendingInsight["confidenceLabel"] {
  if (value === "Low confidence") {
    return "Low confidence";
  }
  if (value === "Medium confidence") {
    return "Medium confidence";
  }
  return "High confidence";
}

function asConfidenceLevel(value: unknown): SpendingInsight["confidenceLevel"] {
  if (value === "LOW") {
    return "LOW";
  }
  if (value === "MEDIUM") {
    return "MEDIUM";
  }
  return "HIGH";
}

function asStatus(value: unknown): SpendingInsight["status"] {
  if (value === "PARTIAL") {
    return "PARTIAL";
  }
  if (value === "INSUFFICIENT_DATA") {
    return "INSUFFICIENT_DATA";
  }
  return "GENERATED";
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
