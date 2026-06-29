import { beforeEach, describe, expect, it, jest } from "@jest/globals";
import { apiClient } from "./api";
import {
  createCustomerProfile,
  deleteCustomerProfile,
  fetchCustomersForAdmin,
  fetchCustomerDetails,
  fetchCustomerProfile,
  updateCustomerContact,
  updateCustomerProfile
} from "./customers";
import {
  createCustomerAccount,
  deleteCustomerAccount,
  fetchAccountDetails,
  fetchAccounts,
  updateCustomerAccount
} from "./accounts";
import {
  fetchRecentTransactions,
  fetchTransactionHistory,
  submitDeposit,
  submitTransfer,
  submitWithdrawal
} from "./transactions";
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

  it("loads admin customer directory", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        items: [
          {
            customerId: "cust-admin-1",
            externalCustomerKey: "ext-admin-1",
            legalName: "Casey Admin",
            primaryEmail: "casey.admin@example.com",
            phoneNumber: "+61 400 000 001",
            status: "ACTIVE",
            createdAtUtc: "2024-05-10T00:00:00Z"
          },
          {
            customerId: "cust-admin-2",
            externalCustomerKey: "ext-admin-2",
            legalName: "Riley Ops",
            primaryEmail: "riley.ops@example.com",
            phoneNumber: "+61 400 000 002",
            status: "SUSPENDED",
            createdAtUtc: "2024-05-11T00:00:00Z"
          }
        ]
      }
    } as never);

    const customers = await fetchCustomersForAdmin(1, 100);

    expect(getMock).toHaveBeenCalledWith("/customers", {
      params: {
        page: 1,
        pageSize: 100
      }
    });
    expect(customers).toHaveLength(2);
    expect(customers[0].customerId).toBe("cust-admin-1");
    expect(customers[1].status).toBe("SUSPENDED");
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

  it("fetches account details by account id", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        accountId: "acc-909",
        accountType: "CHECKING",
        accountNumber: "NB998877665544",
        currencyCode: "USD",
        status: "ACTIVE",
        availableBalance: "250.00",
        currentBalance: "250.00"
      }
    } as never);

    const details = await fetchAccountDetails("acc-909");

    expect(details.accountId).toBe("acc-909");
    expect(details.status).toBe("Active");
  });

  it("updates account nickname and status", async () => {
    const patchMock = jest.spyOn(apiClient, "patch").mockResolvedValue({
      data: {
        accountId: "acc-909",
        accountType: "CHECKING",
        accountNumber: "NB998877665544",
        currencyCode: "USD",
        nickname: "Travel",
        status: "SUSPENDED",
        availableBalance: "250.00",
        currentBalance: "250.00"
      }
    } as never);

    const updated = await updateCustomerAccount({
      accountId: "acc-909",
      nickname: "Travel",
      status: "SUSPENDED"
    });

    expect(patchMock).toHaveBeenCalledWith("/accounts/acc-909", {
      nickname: "Travel",
      status: "SUSPENDED"
    });
    expect(updated.status).toBe("Paused");
  });

  it("deletes account and maps delete response", async () => {
    const deleteMock = jest.spyOn(apiClient, "delete").mockResolvedValue({
      data: {
        status: "DELETED",
        message: "Account removed"
      }
    } as never);

    const result = await deleteCustomerAccount("acc-909");

    expect(deleteMock).toHaveBeenCalledWith("/accounts/acc-909");
    expect(result.status).toBe("DELETED");
  });

  it("creates and retrieves customer via explicit customer id path", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        customerId: "cust-800",
        legalName: "Alex Parker",
        primaryEmail: "alex.parker@example.com",
        phoneNumber: "+61 401 555 000",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);

    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        customerId: "cust-800",
        legalName: "Alex Parker",
        primaryEmail: "alex.parker@example.com",
        phoneNumber: "+61 401 555 000",
        status: "ACTIVE",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);

    const created = await createCustomerProfile({
      externalCustomerKey: "ext-800",
      legalName: "Alex Parker",
      primaryEmail: "alex.parker@example.com",
      phoneNumber: "+61 401 555 000"
    });
    const retrieved = await fetchCustomerDetails("cust-800");

    expect(postMock).toHaveBeenCalledWith("/customers", {
      externalCustomerKey: "ext-800",
      legalName: "Alex Parker",
      primaryEmail: "alex.parker@example.com",
      phoneNumber: "+61 401 555 000"
    });
    expect(getMock).toHaveBeenCalledWith("/customers/cust-800");
    expect(created.customerId).toBe("cust-800");
    expect(retrieved.customerId).toBe("cust-800");
  });

  it("updates and deletes customer by explicit id", async () => {
    const patchMock = jest.spyOn(apiClient, "patch").mockResolvedValue({
      data: {
        customerId: "cust-801",
        legalName: "Alex Parker",
        primaryEmail: "alex.parker@example.com",
        phoneNumber: "+61 422 000 333",
        status: "SUSPENDED",
        createdAtUtc: "2024-03-12T00:00:00Z"
      }
    } as never);

    const deleteMock = jest.spyOn(apiClient, "delete").mockResolvedValue({
      data: {
        status: "DELETED",
        message: "Customer removed"
      }
    } as never);

    const updated = await updateCustomerProfile(
      {
        phoneNumber: "+61 422 000 333",
        status: "SUSPENDED"
      },
      "cust-801"
    );
    const deleted = await deleteCustomerProfile("cust-801");

    expect(patchMock).toHaveBeenCalledWith("/customers/cust-801", {
      legalName: undefined,
      primaryEmail: undefined,
      phoneNumber: "+61 422 000 333",
      status: "SUSPENDED"
    });
    expect(deleteMock).toHaveBeenCalledWith("/customers/cust-801");
    expect(updated.status).toBe("SUSPENDED");
    expect(deleted.status).toBe("DELETED");
  });

  it("lists accounts using explicit customer scope id", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        items: [
          {
            accountId: "acc-admin-1",
            accountType: "CHECKING",
            balance: "400.00",
            status: "ACTIVE"
          }
        ]
      }
    } as never);

    const scopedAccounts = await fetchAccounts("cust-admin-1");

    expect(getMock).toHaveBeenCalledWith("/accounts", {
      params: {
        customerId: "cust-admin-1",
        page: 1,
        pageSize: 20
      }
    });
    expect(scopedAccounts).toHaveLength(1);
    expect(scopedAccounts[0].accountId).toBe("acc-admin-1");
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

  it("submits admin scoped transfer with customer id", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        transferId: "tr-admin-100",
        postedAtUtc: "2026-06-26T10:00:00Z"
      }
    } as never);

    await submitTransfer({
      sourceAccountId: "acc-admin-main",
      destinationAccountId: "acc-admin-dst",
      amount: 22.8,
      customerId: "cust-admin-1"
    });

    expect(postMock).toHaveBeenCalledWith(
      "/transactions/transfer",
      {
        sourceAccountId: "acc-admin-main",
        destinationAccountId: "acc-admin-dst",
        amount: "22.80",
        customerId: "cust-admin-1"
      },
      {
        headers: {
          "Idempotency-Key": expect.stringMatching(/^transfer-/)
        }
      }
    );
  });

  it("submits deposit and maps posting response fields", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        transactionId: "txn-dep-100",
        postedAtUtc: "2026-06-26T11:15:00Z",
        postedAmount: "120.55",
        currencyCode: "USD",
        balanceAfter: "920.55"
      }
    } as never);

    const receipt = await submitDeposit({
      accountId: "acc-main",
      amount: 120.55
    });

    expect(postMock).toHaveBeenCalledWith(
      "/transactions/deposit",
      {
        accountId: "acc-main",
        amount: "120.55"
      },
      {
        headers: {
          "Idempotency-Key": expect.stringMatching(/^deposit-/)
        }
      }
    );
    expect(receipt.reference).toBe("txn-dep-100");
    expect(receipt.transactionType).toBe("DEPOSIT");
    expect(receipt.postedAmount).toBe(120.55);
    expect(receipt.balanceAfter).toBe(920.55);
  });

  it("maps withdrawal insufficient funds to actionable error", async () => {
    jest.spyOn(apiClient, "post").mockRejectedValue({
      response: {
        status: 422,
        data: {
          code: "TRANSACTION_INSUFFICIENT_FUNDS",
          message: "Insufficient funds"
        }
      }
    });

    await expect(
      submitWithdrawal({
        accountId: "acc-main",
        amount: 5000
      })
    ).rejects.toThrow("Insufficient funds for this operation.");
  });

  it("retrieves account scoped history with filters and paging", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockImplementation((url: string, config?: unknown) => {
      if (url === "/customers/me") {
        return Promise.resolve({
          data: {
            customerId: "cust-500",
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
            scopeType: "ACCOUNT",
            scopeId: "acc-777",
            page: 2,
            pageSize: 20,
            transactionType: "TRANSFER_DEBIT",
            startDateUtc: "2026-06-01T00:00:00.000Z",
            endDateUtc: "2026-06-30T23:59:59.999Z"
          }
        });

        return Promise.resolve({
          data: {
            items: [
              {
                transactionId: "txn-200",
                postedAtUtc: "2026-06-22T09:10:00Z",
                transactionType: "TRANSFER_DEBIT",
                amount: "75.00",
                currencyCode: "USD"
              }
            ],
            page: 2,
            pageSize: 20,
            totalItems: 41,
            totalPages: 3
          }
        } as never);
      }

      return Promise.reject(new Error(`Unexpected GET route: ${url}`));
    });

    const history = await fetchTransactionHistory({
      scopeType: "ACCOUNT",
      scopeId: "acc-777",
      transactionType: "TRANSFER_DEBIT",
      startDate: "2026-06-01",
      endDate: "2026-06-30",
      page: 2,
      pageSize: 20
    });

    expect(getMock).toHaveBeenCalledWith("/customers/me");
    expect(getMock).toHaveBeenCalledWith("/transactions/history", {
      params: {
        scopeType: "ACCOUNT",
        scopeId: "acc-777",
        page: 2,
        pageSize: 20,
        transactionType: "TRANSFER_DEBIT",
        startDateUtc: "2026-06-01T00:00:00.000Z",
        endDateUtc: "2026-06-30T23:59:59.999Z"
      }
    });
    expect(history.page).toBe(2);
    expect(history.totalPages).toBe(3);
    expect(history.items[0].transactionType).toBe("TRANSFER_DEBIT");
  });

  it("retrieves customer scoped history using explicit admin customer scope", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockImplementation((url: string, config?: unknown) => {
      if (url === "/transactions/history") {
        expect(config).toEqual({
          params: {
            scopeType: "CUSTOMER",
            scopeId: "cust-admin-2",
            page: 1,
            pageSize: 10
          }
        });

        return Promise.resolve({
          data: {
            items: [],
            page: 1,
            pageSize: 10,
            totalItems: 0,
            totalPages: 1
          }
        } as never);
      }

      return Promise.reject(new Error(`Unexpected GET route: ${url}`));
    });

    const history = await fetchTransactionHistory({
      scopeType: "CUSTOMER",
      customerId: "cust-admin-2",
      page: 1,
      pageSize: 10
    });

    expect(getMock).toHaveBeenCalledWith("/transactions/history", {
      params: {
        scopeType: "CUSTOMER",
        scopeId: "cust-admin-2",
        page: 1,
        pageSize: 10
      }
    });
    expect(history.totalItems).toBe(0);
  });

  it("creates standing order and maps contract response", async () => {
    jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        standingOrderId: "so-100",
        sourceAccountId: "acc-main",
        destinationAccountId: "acc-save",
        amount: "1950.00",
        cadence: "MONTHLY",
        lifecycleState: "ACTIVE",
        nextExecutionAtUtc: "2026-07-01T00:00:00Z",
        effectiveFromUtc: "2026-07-01T00:00:00Z",
        effectiveToUtc: null
      }
    } as never);

    const created = await createStandingOrder({
      sourceAccountId: "acc-main",
      destinationAccountId: "acc-save",
      amount: 1950,
      cadence: "MONTHLY",
      effectiveFromUtc: "2026-07-01T00:00:00Z"
    });

    expect(created.standingOrderId).toBe("so-100");
    expect(created.cadence).toBe("MONTHLY");
    expect(created.lifecycleState).toBe("ACTIVE");
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

    await expect(fetchStatements({ accountId: "acc-main" })).rejects.toThrow("offline");
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
