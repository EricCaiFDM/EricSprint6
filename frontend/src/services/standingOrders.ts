import { apiClient } from "./api";

export type StandingOrderCadence = "DAILY" | "WEEKLY" | "MONTHLY";
export type StandingOrderLifecycleState = "ACTIVE" | "PAUSED" | "CANCELLED" | "COMPLETED";

export type StandingOrder = {
  standingOrderId: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  cadence: StandingOrderCadence;
  lifecycleState: StandingOrderLifecycleState;
  nextExecutionAtUtc: string | null;
  effectiveFromUtc: string;
  effectiveToUtc: string | null;
};

export type CreateStandingOrderInput = {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  cadence: StandingOrderCadence;
  effectiveFromUtc: string;
  effectiveToUtc?: string | null;
  retryPolicyCode?: string;
};

export type UpdateStandingOrderInput = {
  standingOrderId: string;
  amount?: number;
  cadence?: StandingOrderCadence;
  effectiveFromUtc?: string;
  effectiveToUtc?: string | null;
  retryPolicyCode?: string;
};

export type StandingOrderExecutionItem = {
  executionEventId: string;
  dueAtUtc: string;
  startedAtUtc: string;
  completedAtUtc: string | null;
  status: "SUCCEEDED" | "FAILED_INSUFFICIENT_FUNDS" | "FAILED_INELIGIBLE_ACCOUNT" | "FAILED_DEPENDENCY_OUTAGE" | "RETRY_SCHEDULED";
  attemptNumber: number;
  transferReferenceId: string | null;
  reasonCode: string | null;
};

export async function fetchStandingOrders(): Promise<StandingOrder[]> {
  const response = await apiClient.get("/standing-orders", {
    params: {
      page: 1,
      pageSize: 50
    }
  });
  return mapStandingOrders(response.data);
}

export async function createStandingOrder(input: CreateStandingOrderInput): Promise<StandingOrder> {
  const response = await apiClient.post("/standing-orders", {
    sourceAccountId: input.sourceAccountId,
    destinationAccountId: input.destinationAccountId,
    amount: input.amount.toFixed(2),
    cadence: input.cadence,
    effectiveFromUtc: input.effectiveFromUtc,
    effectiveToUtc: input.effectiveToUtc ?? null,
    retryPolicyCode: input.retryPolicyCode
  });

  return mapStandingOrder(response.data);
}

export async function updateStandingOrder(input: UpdateStandingOrderInput): Promise<StandingOrder> {
  const response = await apiClient.patch(`/standing-orders/${encodeURIComponent(input.standingOrderId)}`, {
    amount: typeof input.amount === "number" ? input.amount.toFixed(2) : undefined,
    cadence: input.cadence,
    effectiveFromUtc: input.effectiveFromUtc,
    effectiveToUtc: input.effectiveToUtc ?? undefined,
    retryPolicyCode: input.retryPolicyCode
  });

  return mapStandingOrder(response.data);
}

export async function pauseStandingOrder(standingOrderId: string): Promise<StandingOrder> {
  const response = await apiClient.post(`/standing-orders/${encodeURIComponent(standingOrderId)}/pause`);
  return mapStandingOrder(response.data);
}

export async function resumeStandingOrder(standingOrderId: string): Promise<StandingOrder> {
  const response = await apiClient.post(`/standing-orders/${encodeURIComponent(standingOrderId)}/resume`);
  return mapStandingOrder(response.data);
}

export async function cancelStandingOrder(standingOrderId: string): Promise<StandingOrder> {
  const response = await apiClient.post(`/standing-orders/${encodeURIComponent(standingOrderId)}/cancel`);
  return mapStandingOrder(response.data);
}

export async function fetchStandingOrderExecutions(standingOrderId: string): Promise<StandingOrderExecutionItem[]> {
  const response = await apiClient.get(`/standing-orders/${encodeURIComponent(standingOrderId)}/executions`, {
    params: {
      page: 1,
      pageSize: 20
    }
  });

  return mapExecutionItems(response.data);
}

function mapStandingOrders(payload: unknown): StandingOrder[] {
  if (!payload || typeof payload !== "object") {
    return [];
  }

  const items = Array.isArray((payload as { items?: unknown }).items)
    ? ((payload as { items: unknown[] }).items ?? [])
    : Array.isArray(payload)
      ? payload
      : [];

  return items
    .map((row) => mapStandingOrder(row))
    .filter((order): order is StandingOrder => order !== null);
}

function mapStandingOrder(payload: unknown): StandingOrder {
  if (!payload || typeof payload !== "object") {
    throw new Error("Standing-order payload is invalid");
  }

  const data = payload as Record<string, unknown>;
  const standingOrderId = asString(data.standingOrderId, "");
  if (!standingOrderId) {
    throw new Error("Standing-order payload is missing standingOrderId");
  }

  return {
    standingOrderId,
    sourceAccountId: asString(data.sourceAccountId, ""),
    destinationAccountId: asString(data.destinationAccountId, ""),
    amount: asNumber(data.amount, 0),
    cadence: asCadence(data.cadence),
    lifecycleState: asLifecycleState(data.lifecycleState),
    nextExecutionAtUtc: asNullableString(data.nextExecutionAtUtc),
    effectiveFromUtc: asString(data.effectiveFromUtc, new Date().toISOString()),
    effectiveToUtc: asNullableString(data.effectiveToUtc)
  } satisfies StandingOrder;
}

function mapExecutionItems(payload: unknown): StandingOrderExecutionItem[] {
  if (!payload || typeof payload !== "object") {
    return [];
  }

  const items = Array.isArray((payload as { items?: unknown }).items)
    ? ((payload as { items: unknown[] }).items ?? [])
    : [];

  return items
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      const executionEventId = asString(data.executionEventId, "");
      if (!executionEventId) {
        return null;
      }

      const rawStatus = asString(data.status, "FAILED_DEPENDENCY_OUTAGE");
      if (
        rawStatus !== "SUCCEEDED" &&
        rawStatus !== "FAILED_INSUFFICIENT_FUNDS" &&
        rawStatus !== "FAILED_INELIGIBLE_ACCOUNT" &&
        rawStatus !== "FAILED_DEPENDENCY_OUTAGE" &&
        rawStatus !== "RETRY_SCHEDULED"
      ) {
        return null;
      }

      return {
        executionEventId,
        dueAtUtc: asString(data.dueAtUtc, new Date().toISOString()),
        startedAtUtc: asString(data.startedAtUtc, new Date().toISOString()),
        completedAtUtc: asNullableString(data.completedAtUtc),
        status: rawStatus,
        attemptNumber: Math.max(1, Math.floor(asNumber(data.attemptNumber, 1))),
        transferReferenceId: asNullableString(data.transferReferenceId),
        reasonCode: asNullableString(data.reasonCode)
      } satisfies StandingOrderExecutionItem;
    })
    .filter((item): item is StandingOrderExecutionItem => item !== null);
}

function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asNullableString(value: unknown): string | null {
  return typeof value === "string" && value.trim().length > 0 ? value : null;
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

function asCadence(value: unknown): StandingOrderCadence {
  if (value === "DAILY" || value === "WEEKLY" || value === "MONTHLY") {
    return value;
  }
  if (value === "Daily") {
    return "DAILY";
  }
  if (value === "Weekly") {
    return "WEEKLY";
  }
  return "MONTHLY";
}

function asLifecycleState(value: unknown): StandingOrderLifecycleState {
  if (value === "ACTIVE" || value === "PAUSED" || value === "CANCELLED" || value === "COMPLETED") {
    return value;
  }
  if (value === "Active") {
    return "ACTIVE";
  }
  if (value === "Paused") {
    return "PAUSED";
  }
  return "ACTIVE";
}
