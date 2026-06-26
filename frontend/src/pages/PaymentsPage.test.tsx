import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { PaymentsPage } from "./PaymentsPage";
import * as accounts from "../services/accounts";
import * as transactions from "../services/transactions";

jest.mock("../services/accounts");
jest.mock("../services/transactions");

describe("PaymentsPage", () => {
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
          <PaymentsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders all transaction operation controls", async () => {
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchHistoryMock = transactions.fetchTransactionHistory as jest.MockedFunction<typeof transactions.fetchTransactionHistory>;

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        availableBalance: 1250,
        currentBalance: 1250,
        currency: "USD",
        status: "Active"
      },
      {
        accountId: "acc-2",
        accountName: "Savings",
        accountType: "Savings",
        accountNumberMasked: "**** 5678",
        availableBalance: 3000,
        currentBalance: 3000,
        currency: "USD",
        status: "Active"
      }
    ]);

    fetchHistoryMock.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 10,
      totalItems: 0,
      totalPages: 1
    });

    renderPage();

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalled();
      expect(fetchHistoryMock).toHaveBeenCalled();
    });

    expect(screen.getByRole("heading", { name: /Deposit funds/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Withdraw funds/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Transfer funds/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Transaction history/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Submit deposit/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Submit withdrawal/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Submit transfer/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Apply filters/i })).toBeInTheDocument();
  });

  it("submits deposit for selected account", async () => {
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchHistoryMock = transactions.fetchTransactionHistory as jest.MockedFunction<typeof transactions.fetchTransactionHistory>;
    const submitDepositMock = transactions.submitDeposit as jest.MockedFunction<typeof transactions.submitDeposit>;

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        availableBalance: 1250,
        currentBalance: 1250,
        currency: "USD",
        status: "Active"
      },
      {
        accountId: "acc-2",
        accountName: "Savings",
        accountType: "Savings",
        accountNumberMasked: "**** 5678",
        availableBalance: 3000,
        currentBalance: 3000,
        currency: "USD",
        status: "Active"
      }
    ]);

    fetchHistoryMock.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 10,
      totalItems: 0,
      totalPages: 1
    });

    submitDepositMock.mockResolvedValue({
      reference: "txn-1",
      transactionType: "DEPOSIT",
      status: "Completed",
      submittedAt: "2026-06-26T11:15:00Z",
      postedAmount: 125.5,
      currency: "USD",
      balanceAfter: 1375.5
    });

    renderPage();

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalled();
    });

    fireEvent.change(screen.getAllByLabelText(/^Account$/i)[0], {
      target: { value: "acc-1" }
    });

    fireEvent.change(screen.getAllByLabelText(/^Amount$/i)[0], {
      target: { value: "125.5" }
    });

    const depositButton = screen.getByRole("button", { name: /Submit deposit/i });
    await waitFor(() => {
      expect(depositButton).not.toBeDisabled();
    });
    fireEvent.click(depositButton);

    await waitFor(() => {
      expect(submitDepositMock).toHaveBeenCalled();
    });

    const firstCall = submitDepositMock.mock.calls[0]?.[0];
    expect(firstCall?.accountId).toBe("acc-1");
    expect(firstCall?.amount).toBeCloseTo(125.5);
    expect(firstCall?.customerId).toBeUndefined();

    expect(await screen.findByText(/Deposit completed\. Reference txn-1\./i)).toBeInTheDocument();
  });
});
