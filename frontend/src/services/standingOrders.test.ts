import { beforeEach, describe, expect, it, jest } from "@jest/globals";

import { apiClient } from "./api";
import {
  cancelStandingOrder,
  createStandingOrder,
  fetchStandingOrderExecutions,
  fetchStandingOrders,
  pauseStandingOrder,
  resumeStandingOrder,
  updateStandingOrder
} from "./standingOrders";

describe("standingOrders service", () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  it("fetchStandingOrders maps paged payload", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        items: [
          {
            standingOrderId: "so-100",
            sourceAccountId: "acc-a",
            destinationAccountId: "acc-b",
            amount: "75.50",
            cadence: "WEEKLY",
            lifecycleState: "ACTIVE",
            nextExecutionAtUtc: "2026-06-30T00:00:00Z",
            effectiveFromUtc: "2026-06-01T00:00:00Z",
            effectiveToUtc: null
          }
        ],
        page: 1,
        pageSize: 50,
        totalItems: 1,
        totalPages: 1
      }
    } as never);

    const result = await fetchStandingOrders();

    expect(result).toHaveLength(1);
    expect(result[0]).toMatchObject({
      standingOrderId: "so-100",
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: 75.5,
      cadence: "WEEKLY",
      lifecycleState: "ACTIVE"
    });
  });

  it("createStandingOrder posts contract-compliant payload", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        standingOrderId: "so-101",
        sourceAccountId: "acc-a",
        destinationAccountId: "acc-b",
        amount: "25.00",
        cadence: "MONTHLY",
        lifecycleState: "ACTIVE",
        nextExecutionAtUtc: "2026-07-01T00:00:00Z",
        effectiveFromUtc: "2026-06-01T00:00:00Z",
        effectiveToUtc: null
      }
    } as never);

    const result = await createStandingOrder({
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: 25,
      cadence: "MONTHLY",
      effectiveFromUtc: "2026-06-01T00:00:00Z",
      retryPolicyCode: "STANDARD"
    });

    expect(postMock).toHaveBeenCalledWith("/standing-orders", {
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: "25.00",
      cadence: "MONTHLY",
      effectiveFromUtc: "2026-06-01T00:00:00Z",
      effectiveToUtc: null,
      retryPolicyCode: "STANDARD"
    });

    expect(result.standingOrderId).toBe("so-101");
  });

  it("updateStandingOrder formats decimal amount", async () => {
    const patchMock = jest.spyOn(apiClient, "patch").mockResolvedValue({
      data: {
        standingOrderId: "so-102",
        sourceAccountId: "acc-a",
        destinationAccountId: "acc-b",
        amount: "45.75",
        cadence: "DAILY",
        lifecycleState: "ACTIVE",
        nextExecutionAtUtc: "2026-06-26T00:00:00Z",
        effectiveFromUtc: "2026-06-25T00:00:00Z",
        effectiveToUtc: null
      }
    } as never);

    await updateStandingOrder({
      standingOrderId: "so-102",
      amount: 45.75,
      cadence: "DAILY"
    });

    expect(patchMock).toHaveBeenCalledWith("/standing-orders/so-102", {
      amount: "45.75",
      cadence: "DAILY",
      effectiveFromUtc: undefined,
      effectiveToUtc: undefined,
      retryPolicyCode: undefined
    });
  });

  it("lifecycle helpers call expected endpoints", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        standingOrderId: "so-103",
        sourceAccountId: "acc-a",
        destinationAccountId: "acc-b",
        amount: "12.00",
        cadence: "MONTHLY",
        lifecycleState: "PAUSED",
        nextExecutionAtUtc: null,
        effectiveFromUtc: "2026-06-25T00:00:00Z",
        effectiveToUtc: null
      }
    } as never);

    await pauseStandingOrder("so-103");
    await resumeStandingOrder("so-103");
    await cancelStandingOrder("so-103");

    expect(postMock).toHaveBeenNthCalledWith(1, "/standing-orders/so-103/pause");
    expect(postMock).toHaveBeenNthCalledWith(2, "/standing-orders/so-103/resume");
    expect(postMock).toHaveBeenNthCalledWith(3, "/standing-orders/so-103/cancel");
  });

  it("fetchStandingOrderExecutions maps execution list", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        items: [
          {
            executionEventId: "evt-1",
            dueAtUtc: "2026-06-25T10:00:00Z",
            startedAtUtc: "2026-06-25T10:00:01Z",
            completedAtUtc: "2026-06-25T10:00:02Z",
            status: "SUCCEEDED",
            attemptNumber: 1,
            transferReferenceId: "trf-1",
            reasonCode: null
          }
        ]
      }
    } as never);

    const result = await fetchStandingOrderExecutions("so-104");

    expect(result).toHaveLength(1);
    expect(result[0]).toMatchObject({
      executionEventId: "evt-1",
      status: "SUCCEEDED",
      attemptNumber: 1,
      transferReferenceId: "trf-1"
    });
  });
});
