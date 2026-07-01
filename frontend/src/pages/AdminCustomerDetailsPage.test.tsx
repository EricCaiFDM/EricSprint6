import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import { AdminCustomerDetailsPage } from "./AdminCustomerDetailsPage";
import * as accounts from "../services/accounts";
import * as customers from "../services/customers";
import * as insights from "../services/insights";
import * as statements from "../services/statements";
import * as transactions from "../services/transactions";
import { formatStatementPeriod } from "../utils/formatting";

jest.mock("../services/accounts");
jest.mock("../services/customers");
jest.mock("../services/insights", () => {
  const actual = jest.requireActual("../services/insights");
  return {
    ...actual,
    fetchSpendingInsights: jest.fn()
  };
});
jest.mock("../services/statements");
jest.mock("../services/transactions");

function expectedMonthQuery(periodYearMonth: string): { periodStartUtc: string; periodEndUtc: string } {
  const [yearPart, monthPart] = periodYearMonth.split("-");
  const year = Number(yearPart);
  const monthIndex = Number(monthPart) - 1;

  const startLocal = new Date(year, monthIndex, 1, 0, 0, 0, 0);
  const endExclusiveLocal = new Date(year, monthIndex + 1, 1, 0, 0, 0, 0);

  return {
    periodStartUtc: startLocal.toISOString(),
    periodEndUtc: endExclusiveLocal.toISOString()
  };
}

describe("AdminCustomerDetailsPage", () => {
  function renderPage(initialPath = "/admin/customers/cust-77") {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false
        }
      }
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route path="/admin/customers/:customerId" element={<AdminCustomerDetailsPage />} />
            <Route path="/admin/dashboard" element={<div>Admin dashboard route</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    jest.setSystemTime(new Date("2026-07-15T12:00:00.000Z"));

    (customers.fetchCustomerDetails as jest.MockedFunction<typeof customers.fetchCustomerDetails>).mockResolvedValue({
      customerId: "cust-77",
      fullName: "Alex Morgan",
      email: "alex@example.com",
      mobile: "+61 411 222 333",
      status: "ACTIVE",
      joinedAt: "2024-04-10T00:00:00Z"
    });

    (accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>).mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Primary Checking",
        accountType: "Everyday",
        accountNumberMasked: "**** 1001",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1500,
        currentBalance: 1500,
        currency: "USD",
        status: "Active"
      },
      {
        accountId: "acc-2",
        accountName: "Emergency Savings",
        accountType: "Savings",
        accountNumberMasked: "**** 2002",
        checkingNumber: null,
        interestRate: 2.1,
        availableBalance: 4200,
        currentBalance: 4200,
        currency: "USD",
        status: "Active"
      }
    ]);

    (transactions.fetchTransactionHistory as jest.MockedFunction<typeof transactions.fetchTransactionHistory>).mockResolvedValue({
      items: [
        {
          transactionId: "txn-1",
          accountId: "acc-1",
          transactionType: "DEPOSIT",
          bookedAt: "2026-06-29T08:30:00Z",
          description: "Paycheck",
          category: "Income",
          amount: 1800,
          currency: "USD",
          direction: "CREDIT",
          status: "Completed"
        }
      ],
      page: 1,
      pageSize: 20,
      totalItems: 1,
      totalPages: 1
    });

    (insights.fetchSpendingInsights as jest.MockedFunction<typeof insights.fetchSpendingInsights>).mockResolvedValue({
      periodLabel: "June 2026",
      periodStartUtc: "2026-06-01T00:00:00Z",
      periodEndUtc: "2026-06-30T23:59:59Z",
      scopeType: "CUSTOMER",
      scopeId: "cust-77",
      totalSpend: 930.4,
      currency: "USD",
      confidenceLabel: "Medium confidence",
      confidenceLevel: "MEDIUM",
      coverageRatio: 82.5,
      confidenceReason: "Enough posted spending activity is available for directional analysis.",
      status: "GENERATED",
      methodology: "Insights group posted debits into approved category taxonomy.",
      categories: [
        {
          category: "Groceries",
          amount: 320,
          ratio: 0.34,
          trend: "flat"
        }
      ]
    });

    (statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>)
      .mockResolvedValueOnce({
        items: [
          {
            statementId: "stmt-1",
            accountId: "acc-1",
            periodYearMonth: "2026-06",
            artifactVersion: 1,
            status: "GENERATED",
            generatedAtUtc: "2026-06-30T00:05:00Z"
          }
        ],
        page: 1,
        pageSize: 6,
        totalItems: 1,
        totalPages: 1
      })
      .mockResolvedValueOnce({
        items: [],
        page: 1,
        pageSize: 6,
        totalItems: 0,
        totalPages: 1
      });
  });

  it("renders customer profile, accounts, transactions, statements, and spending insights", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { name: /Customer profile overview/i })).toBeInTheDocument();
    expect(await screen.findByText(/Alex Morgan/i)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Spending insights/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Insight month/i)).toHaveValue("2026-07");
    expect(screen.getByText(/Spending period:/i)).toBeInTheDocument();
    expect(screen.getByText(/Groceries/i)).toBeInTheDocument();

    expect(screen.getByRole("heading", { name: /^Accounts$/i })).toBeInTheDocument();
    expect(screen.getAllByText(/Primary Checking/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Emergency Savings/i).length).toBeGreaterThan(0);

    expect(screen.getByRole("heading", { name: /Transaction history/i })).toBeInTheDocument();
    expect(await screen.findByText(/Paycheck/i)).toBeInTheDocument();

    expect(screen.getByRole("heading", { name: /Monthly statements/i })).toBeInTheDocument();
    expect(await screen.findByText(`${formatStatementPeriod("2026-06")} · Version 1`)).toBeInTheDocument();
    expect(await screen.findByText(/No statements found for this account./i)).toBeInTheDocument();

    await waitFor(() => {
      expect(transactions.fetchTransactionHistory).toHaveBeenCalledWith({
        scopeType: "ACCOUNT",
        scopeId: "acc-1",
        page: 1,
        pageSize: 20
      });
      expect(statements.fetchStatements).toHaveBeenCalledWith({
        accountId: "acc-1",
        page: 1,
        pageSize: 6
      });
      expect(statements.fetchStatements).toHaveBeenCalledWith({
        accountId: "acc-2",
        page: 1,
        pageSize: 6
      });
      expect(insights.fetchSpendingInsights).toHaveBeenCalledWith({
        scopeType: "CUSTOMER",
        scopeId: "cust-77",
        ...expectedMonthQuery("2026-07")
      });
    });
  });

  it("allows admin to choose a specific insights month", async () => {
    renderPage();

    await waitFor(() => {
      expect(insights.fetchSpendingInsights).toHaveBeenCalledWith({
        scopeType: "CUSTOMER",
        scopeId: "cust-77",
        ...expectedMonthQuery("2026-07")
      });
    });

    fireEvent.change(screen.getByLabelText(/Insight month/i), {
      target: { value: "2026-05" }
    });

    await waitFor(() => {
      expect(insights.fetchSpendingInsights).toHaveBeenLastCalledWith({
        scopeType: "CUSTOMER",
        scopeId: "cust-77",
        ...expectedMonthQuery("2026-05")
      });
    });
  });

  it("shows a profile loading error while preserving other sections", async () => {
    (customers.fetchCustomerDetails as jest.MockedFunction<typeof customers.fetchCustomerDetails>)
      .mockRejectedValueOnce(new Error("Customer lookup failed"));

    renderPage();

    expect(await screen.findByText(/Unable to load customer profile: Customer lookup failed/i)).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: /Spending insights/i })).toBeInTheDocument();
  });

  afterEach(() => {
    jest.useRealTimers();
  });
});
