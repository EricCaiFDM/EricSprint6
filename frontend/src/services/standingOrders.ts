import { apiClient } from "./api";

export type StandingOrder = {
  standingOrderId: string;
  payeeName: string;
  sourceAccountName: string;
  amount: number;
  currency: string;
  frequency: "Weekly" | "Fortnightly" | "Monthly";
  nextRunAt: string;
  status: "Active" | "Paused";
};

export type CreateStandingOrderInput = {
  payeeName: string;
  sourceAccountId: string;
  amount: number;
  frequency: StandingOrder["frequency"];
  nextRunAt: string;
};

export async function fetchStandingOrders(): Promise<StandingOrder[]> {
  const response = await apiClient.get("/standing-orders");
  return mapStandingOrders(response.data);
}

export async function createStandingOrder(input: CreateStandingOrderInput): Promise<StandingOrder> {
  const response = await apiClient.post("/standing-orders", {
    sourceAccountId: input.sourceAccountId,
    amount: input.amount,
    currency: "AUD",
    frequency: input.frequency.toUpperCase(),
    startDateUtc: input.nextRunAt,
    payeeName: input.payeeName
  });

  return {
    standingOrderId: asString(response.data?.standingOrderId, `so-${Date.now()}`),
    payeeName: input.payeeName,
    sourceAccountName: "Selected Account",
    amount: input.amount,
    currency: "AUD",
    frequency: input.frequency,
    nextRunAt: input.nextRunAt,
    status: "Active"
  };
}

function mapStandingOrders(payload: unknown): StandingOrder[] {
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      const standingOrderId = asString(data.standingOrderId, "");
      if (!standingOrderId) {
        return null;
      }

      return {
        standingOrderId,
        payeeName: asString(data.payeeName, "Scheduled payee"),
        sourceAccountName: asString(data.sourceAccountName, "Everyday Banking"),
        amount: asNumber(data.amount, 0),
        currency: asString(data.currency, "AUD"),
        frequency: asFrequency(data.frequency),
        nextRunAt: asString(data.nextRunAt, new Date().toISOString()),
        status: asString(data.status, "Active") === "Paused" ? "Paused" : "Active"
      } satisfies StandingOrder;
    })
    .filter((order): order is StandingOrder => order !== null);
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

function asFrequency(value: unknown): StandingOrder["frequency"] {
  if (value === "Weekly" || value === "Fortnightly" || value === "Monthly") {
    return value;
  }
  if (value === "WEEKLY") {
    return "Weekly";
  }
  if (value === "FORTNIGHTLY") {
    return "Fortnightly";
  }
  return "Monthly";
}
