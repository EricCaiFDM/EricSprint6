import { beforeEach, describe, expect, it, jest } from "@jest/globals";
import { apiClient } from "./api";
import { fetchCustomerProfile, updateCustomerContact } from "./customers";
import { fetchAccounts } from "./accounts";
import { fetchRecentTransactions, submitTransfer } from "./transactions";
import { createStandingOrder } from "./standingOrders";
import { fetchNotificationPreferences } from "./notifications";
import { fetchStatements } from "./statements";
import { fetchSpendingInsights } from "./insights";

describe("customer experience services", () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  it("returns fallback profile when profile request fails", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue(new Error("offline"));

    const profile = await fetchCustomerProfile();

    expect(profile.email).toBe("jordan.patel@example.com");
    expect(profile.firstName).toBe("Jordan");
  });

  it("applies contact updates in fallback mode", async () => {
    jest.spyOn(apiClient, "patch").mockRejectedValue(new Error("offline"));

    const updated = await updateCustomerContact({
      mobile: "+61 499 001 002",
      addressLine1: "22 River Walk",
      city: "Melbourne",
      postalCode: "3000"
    });

    expect(updated.mobile).toBe("+61 499 001 002");
    expect(updated.addressLine1).toBe("22 River Walk");
    expect(updated.city).toBe("Melbourne");
    expect(updated.postalCode).toBe("3000");
  });

  it("maps account payload into customer account cards", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: [{ accountId: "acc-1", accountType: "CHECKING", balance: "42.50" }]
    } as never);

    const accounts = await fetchAccounts();

    expect(accounts).toHaveLength(1);
    expect(accounts[0].accountType).toBe("Everyday");
    expect(accounts[0].availableBalance).toBe(42.5);
    expect(accounts[0].status).toBe("Active");
  });

  it("maps transaction signs into customer debit and credit directions", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: [
        { transactionId: "txn-1", amount: -19.2, description: "Coffee" },
        { transactionId: "txn-2", amount: 80, description: "Refund" }
      ]
    } as never);

    const transactions = await fetchRecentTransactions();

    expect(transactions).toHaveLength(2);
    expect(transactions[0].direction).toBe("DEBIT");
    expect(transactions[0].amount).toBe(19.2);
    expect(transactions[1].direction).toBe("CREDIT");
  });

  it("submits transfer and returns backend payment reference", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: { reference: "NB-REF-1001" }
    } as never);

    const receipt = await submitTransfer({
      sourceAccountId: "acc-main",
      destinationAccountId: "dest-1",
      recipientName: "Alex Morgan",
      amount: 12.4,
      note: "Lunch"
    });

    expect(postMock).toHaveBeenCalledWith("/transactions/transfer", {
      sourceAccountId: "acc-main",
      destinationAccountId: "dest-1",
      amount: 12.4,
      currency: "AUD",
      note: "Lunch",
      recipientName: "Alex Morgan"
    });
    expect(receipt.reference).toBe("NB-REF-1001");
    expect(receipt.status).toBe("Submitted");
  });

  it("creates recurring payment and maps response id", async () => {
    jest.spyOn(apiClient, "post").mockResolvedValue({
      data: { standingOrderId: "so-100" }
    } as never);

    const created = await createStandingOrder({
      payeeName: "Citywide Rent",
      sourceAccountId: "acc-main",
      amount: 1950,
      frequency: "Monthly",
      nextRunAt: "2026-07-01"
    });

    expect(created.standingOrderId).toBe("so-100");
    expect(created.frequency).toBe("Monthly");
    expect(created.status).toBe("Active");
  });

  it("reads notification preferences from API response", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        pushEnabled: false,
        emailEnabled: true,
        smsEnabled: true,
        marketingEnabled: false
      }
    } as never);

    const preferences = await fetchNotificationPreferences();

    expect(preferences.pushEnabled).toBe(false);
    expect(preferences.emailEnabled).toBe(true);
    expect(preferences.smsEnabled).toBe(true);
    expect(preferences.marketingEnabled).toBe(false);
  });

  it("returns fallback statements when statement endpoint is unavailable", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue(new Error("offline"));

    const statements = await fetchStatements();

    expect(statements.length).toBeGreaterThan(0);
    expect(statements[0].statementId).toBe("stmt-2026-05");
  });

  it("maps insight response and keeps customer confidence metadata", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        periodLabel: "June 2026",
        totalSpend: "1234.50",
        currency: "AUD",
        confidenceLabel: "Medium confidence",
        categories: [{ category: "Travel", amount: 240, ratio: 0.19, trend: "up" }]
      }
    } as never);

    const insights = await fetchSpendingInsights();

    expect(insights.totalSpend).toBe(1234.5);
    expect(insights.confidenceLabel).toBe("Medium confidence");
    expect(insights.categories[0].category).toBe("Travel");
    expect(insights.categories[0].trend).toBe("up");
  });
});
