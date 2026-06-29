import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { DashboardPage } from "./DashboardPage";
import * as accounts from "../services/accounts";
import * as standingOrders from "../services/standingOrders";
import * as transactions from "../services/transactions";

jest.mock("../services/accounts");
jest.mock("../services/standingOrders");
jest.mock("../services/transactions");

describe("DashboardPage", () => {
  function renderPage() {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false
        }
      }
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();

    (accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>).mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        availableBalance: 1250,
        currentBalance: 1250,
        currency: "AUD",
        status: "Active"
      }
    ]);

    (transactions.fetchRecentTransactions as jest.MockedFunction<typeof transactions.fetchRecentTransactions>).mockResolvedValue([
      {
        transactionId: "txn-1",
        accountId: "acc-1",
        transactionType: "DEPOSIT",
        bookedAt: "2026-06-29T08:30:00Z",
        description: "Salary",
        category: "Income",
        amount: 2400,
        currency: "AUD",
        direction: "CREDIT",
        status: "Completed"
      }
    ]);

    (standingOrders.fetchStandingOrders as jest.MockedFunction<typeof standingOrders.fetchStandingOrders>).mockResolvedValue([]);
  });

  it("shows account names and IDs in recent activity", async () => {
    renderPage();

    expect(await screen.findByText(/Salary/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText(/Account: Everyday \(acc-1\)/i)).toBeInTheDocument();
    });
  });
});
