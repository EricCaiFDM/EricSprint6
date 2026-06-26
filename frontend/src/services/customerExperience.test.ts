import { beforeEach, describe, expect, it, jest } from "@jest/globals";
import { apiClient } from "./api";
import { fetchCustomerProfile, updateCustomerContact } from "./customers";
import { createCustomerAccount, fetchAccounts } from "./accounts";
import { fetchRecentTransactions, submitTransfer } from "./transactions";
import { createStandingOrder } from "./standingOrders";
import { fetchNotificationPreferences } from "./notifications";
import { fetchStatements } from "./statements";
import { fetchSpendingInsights } from "./insights";

describe("customer experience services", () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    window.localStorage.clear();
  });

  it("loads customer profile from customer route", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-100");
    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        customerId: "cust-100",
        legalName: "Jordan Patel",
        primaryEmail: "jordan.patel@example.com",
        phoneNumber: "+61 412 345 678",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);

    const profile = await fetchCustomerProfile();

    expect(getMock).toHaveBeenCalledWith("/customers/cust-100");
    expect(profile.customerId).toBe("cust-100");
    expect(profile.email).toBe("jordan.patel@example.com");
    expect(profile.fullName).toBe("Jordan Patel");
  });

  it("updates customer contact through customer patch route", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-101");
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        customerId: "cust-101",
        legalName: "Jordan Patel",
        primaryEmail: "jordan.patel@example.com",
        phoneNumber: "+61 412 345 678",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);
    const patchMock = jest.spyOn(apiClient, "patch").mockResolvedValue({
      data: {
        customerId: "cust-101",
        legalName: "Jordan Patel",
        primaryEmail: "jordan.patel@example.com",
        phoneNumber: "+61 499 001 002",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);

    const updated = await updateCustomerContact({
      phoneNumber: "+61 499 001 002"
    });

    expect(patchMock).toHaveBeenCalledWith("/customers/cust-101", {
      phoneNumber: "+61 499 001 002"
    });
    expect(updated.mobile).toBe("+61 499 001 002");
  });

  it("maps account payload into customer account cards", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-102");
    const getMock = jest.spyOn(apiClient, "get").mockImplementation((url: string, config?: unknown) => {
      if (url === "/customers/cust-102") {
        return Promise.resolve({
          data: {
            customerId: "cust-102",
            legalName: "Jordan Patel",
            primaryEmail: "jordan.patel@example.com",
            phoneNumber: "+61 412 345 678",
            status: "ACTIVE",
            createdAtUtc: "2024-03-12T00:00:00Z"
          }
        } as never);
      }

      if (url === "/accounts") {
        expect(config).toEqual({
          params: {
            customerId: "cust-102",
            page: 1,
            pageSize: 20
          }
        });
        return Promise.resolve({
          data: {
            items: [{ accountId: "acc-1", accountType: "CHECKING", balance: "42.50", status: "ACTIVE" }]
          }
        } as never);
      }

      return Promise.reject(new Error(`Unexpected GET route: ${url}`));
    });

    const accounts = await fetchAccounts();

    expect(getMock).toHaveBeenCalledWith("/customers/cust-102");
    expect(getMock).toHaveBeenCalledWith("/accounts", {
      params: {
        customerId: "cust-102",
        page: 1,
        pageSize: 20
      }
    });
    expect(accounts).toHaveLength(1);
    expect(accounts[0].accountType).toBe("Everyday");
    expect(accounts[0].availableBalance).toBe(42.5);
    expect(accounts[0].status).toBe("Active");
  });

  it("recovers from stale cached customer id before loading accounts", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-stale-1");
    const getMock = jest.spyOn(apiClient, "get").mockImplementation((url: string, config?: unknown) => {
      if (url === "/customers/cust-stale-1") {
        return Promise.reject({
          response: {
            status: 403,
            data: {
              code: "CUSTOMER_FORBIDDEN",
              message: "Insufficient privileges to read customer"
            }
          }
        });
      }

      if (url === "/customers/me") {
        return Promise.resolve({
          data: {
            customerId: "cust-live-1",
            legalName: "Jordan Patel",
            primaryEmail: "jordan.patel@example.com",
            phoneNumber: "+61 412 345 678",
            status: "ACTIVE",
            createdAtUtc: "2024-03-12T00:00:00Z"
          }
        } as never);
      }

      if (url === "/accounts") {
        expect(config).toEqual({
          params: {
            customerId: "cust-live-1",
            page: 1,
            pageSize: 20
          }
        });
        return Promise.resolve({
          data: {
            items: []
          }
        } as never);
      }

      return Promise.reject(new Error(`Unexpected GET route: ${url}`));
    });

    const accounts = await fetchAccounts();

    expect(accounts).toHaveLength(0);
    expect(window.localStorage.getItem("nb_customer_id")).toBe("cust-live-1");
    expect(getMock).toHaveBeenCalledWith("/customers/cust-stale-1");
    expect(getMock).toHaveBeenCalledWith("/customers/me");
  });

  it("resolves active customer id via current profile when local customer context is missing", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockImplementation((url: string, config?: unknown) => {
      if (url === "/customers/me") {
        return Promise.resolve({
          data: {
            customerId: "cust-me-200",
            legalName: "Jordan Patel",
            primaryEmail: "jordan.patel@example.com",
            phoneNumber: "+61 412 345 678",
            status: "ACTIVE",
            createdAtUtc: "2024-03-12T00:00:00Z"
          }
        } as never);
      }

      if (url === "/accounts") {
        expect(config).toEqual({
          params: {
            customerId: "cust-me-200",
            page: 1,
            pageSize: 20
          }
        });

        return Promise.resolve({
          data: {
            items: []
          }
        } as never);
      }

      return Promise.reject(new Error(`Unexpected GET route: ${url}`));
    });

    const accounts = await fetchAccounts();

    expect(accounts).toHaveLength(0);
    expect(window.localStorage.getItem("nb_customer_id")).toBe("cust-me-200");
    expect(getMock).toHaveBeenCalledWith("/customers/me");
  });

  it("shows actionable message when account list fails due scope mismatch", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-102");
    jest.spyOn(apiClient, "get").mockImplementation((url: string) => {
      if (url === "/customers/cust-102") {
        return Promise.resolve({
          data: {
            customerId: "cust-102",
            legalName: "Jordan Patel",
            primaryEmail: "jordan.patel@example.com",
            phoneNumber: "+61 412 345 678",
            status: "ACTIVE",
            createdAtUtc: "2024-03-12T00:00:00Z"
          }
        } as never);
      }

      return Promise.reject({
        response: {
          status: 403,
          data: {
            code: "ACCOUNT_FORBIDDEN",
            message: "Insufficient privileges to list account"
          }
        }
      });
    });

    await expect(fetchAccounts()).rejects.toThrow(
      "This signed-in account is not authorized for the selected customer accounts. Sign out and sign in with the correct account, then retry."
    );
  });

  it("shows setup guidance when signed-in account has no linked customer profile", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue({
      response: {
        status: 404,
        data: {
          code: "CUSTOMER_NOT_FOUND",
          message: "No customer account record found for this sign-in. Complete account setup first."
        }
      }
    });

    await expect(fetchAccounts()).rejects.toThrow(
      "No customer account record found for this sign-in. Complete account setup first."
    );
  });

  it("creates customer account using account route payload", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-103");
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        customerId: "cust-103",
        legalName: "Jordan Patel",
        primaryEmail: "jordan.patel@example.com",
        phoneNumber: "+61 412 345 678",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        accountId: "acc-101",
        accountType: "SAVINGS",
        accountNumber: "NB123456789012",
        currencyCode: "USD",
        nickname: "Rainy Day",
        status: "ACTIVE"
      }
    } as never);

    const created = await createCustomerAccount({
      accountType: "SAVINGS",
      currencyCode: "usd",
      nickname: "Rainy Day"
    });

    expect(postMock).toHaveBeenCalledWith("/accounts", {
      customerId: "cust-103",
      accountType: "SAVINGS",
      currencyCode: "USD",
      nickname: "Rainy Day"
    });
    expect(created.accountId).toBe("acc-101");
    expect(created.accountType).toBe("Savings");
    expect(created.status).toBe("Active");
  });

  it("shows actionable message when account creation fails due missing customer profile", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-104");
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        customerId: "cust-104",
        legalName: "Jordan Patel",
        primaryEmail: "jordan.patel@example.com",
        phoneNumber: "+61 412 345 678",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);
    jest.spyOn(apiClient, "post").mockRejectedValue({
      response: {
        status: 404,
        data: {
          code: "CUSTOMER_NOT_FOUND",
          message: "No customer found with the provided customerId"
        }
      }
    });

    await expect(
      createCustomerAccount({
        accountType: "CHECKING",
        currencyCode: "USD"
      })
    ).rejects.toThrow("No customer account record found for this sign-in. Complete account setup first.");
  });

  it("maps transaction signs into customer debit and credit directions", async () => {
    window.localStorage.setItem("nb_customer_id", "cust-105");
    const getMock = jest.spyOn(apiClient, "get").mockImplementation((url: string, config?: unknown) => {
      if (url === "/customers/cust-105") {
        return Promise.resolve({
          data: {
            customerId: "cust-105",
            legalName: "Jordan Patel",
            primaryEmail: "jordan.patel@example.com",
            phoneNumber: "+61 412 345 678",
            status: "ACTIVE",
            createdAtUtc: "2024-03-12T00:00:00Z"
          }
        } as never);
      }

      if (url === "/transactions/history") {
        expect(config).toEqual({
          params: {
            scopeType: "CUSTOMER",
            scopeId: "cust-105",
            page: 1,
            pageSize: 8
          }
        });

        return Promise.resolve({
          data: {
            items: [
              {
                transactionId: "txn-1",
                postedAtUtc: "2026-06-26T08:00:00Z",
                transactionType: "WITHDRAWAL",
                amount: "19.20",
                currencyCode: "USD"
              },
              {
                transactionId: "txn-2",
                postedAtUtc: "2026-06-26T09:00:00Z",
                transactionType: "DEPOSIT",
                amount: "80.00",
                currencyCode: "USD"
              }
            ]
          }
        } as never);
      }

      return Promise.reject(new Error(`Unexpected GET route: ${url}`));
    });

    const transactions = await fetchRecentTransactions();

    expect(getMock).toHaveBeenCalledWith("/customers/cust-105");
    expect(transactions).toHaveLength(2);
    expect(transactions[0].direction).toBe("DEBIT");
    expect(transactions[0].amount).toBe(19.2);
    expect(transactions[1].direction).toBe("CREDIT");
  });

  it("submits transfer and returns backend payment reference", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        transferId: "tr-1001",
        postedAtUtc: "2026-06-26T10:00:00Z"
      }
    } as never);

    const receipt = await submitTransfer({
      sourceAccountId: "acc-main",
      destinationAccountId: "dest-1",
      amount: 12.4,
      note: "Lunch"
    });

    expect(postMock).toHaveBeenCalledWith(
      "/transactions/transfer",
      {
        sourceAccountId: "acc-main",
        destinationAccountId: "dest-1",
        amount: "12.40"
      },
      {
        headers: {
          "Idempotency-Key": expect.stringMatching(/^transfer-/)
        }
      }
    );
    expect(receipt.reference).toBe("tr-1001");
    expect(receipt.status).toBe("Completed");
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

  it("throws when statement endpoint is unavailable", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue(new Error("offline"));

    await expect(fetchStatements()).rejects.toThrow("offline");
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
