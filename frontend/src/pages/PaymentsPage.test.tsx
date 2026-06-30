import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { PaymentsPage } from "./PaymentsPage";
import * as accounts from "../services/accounts";
import * as customers from "../services/customers";
import * as notifications from "../services/notifications";
import * as session from "../services/session";
import * as transactions from "../services/transactions";

jest.mock("../services/accounts");
jest.mock("../services/customers");
jest.mock("../services/notifications");
jest.mock("../services/session");
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

    (session.getNormalizedTokenRole as jest.MockedFunction<typeof session.getNormalizedTokenRole>).mockReturnValue("CUSTOMER");

    (customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>).mockResolvedValue([
      {
        customerId: "cust-200",
        externalCustomerKey: "ext-200",
        fullName: "Casey Admin",
        email: "casey@example.com",
        mobile: "+61 400 000 200",
        status: "ACTIVE",
        joinedAt: "2024-05-01T00:00:00Z"
      }
    ]);

    (notifications.fetchRecentNotifications as jest.MockedFunction<typeof notifications.fetchRecentNotifications>).mockResolvedValue([
      {
        notificationId: "notif-1",
        title: "Deposit Posted",
        message: "Delivered successfully",
        occurredAt: "2026-06-29T09:30:00Z",
        level: "Info"
      }
    ]);
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
        checkingNumber: 1,
        interestRate: 0,
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
        checkingNumber: null,
        interestRate: 2.5,
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
        checkingNumber: 1,
        interestRate: 0,
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
        checkingNumber: null,
        interestRate: 2.5,
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
    expect(await screen.findByRole("alert")).toHaveTextContent(/Notification sent: Deposit Posted\./i);
  });

  it("shows transaction account labels with IDs and final total account balance", async () => {
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchHistoryMock = transactions.fetchTransactionHistory as jest.MockedFunction<typeof transactions.fetchTransactionHistory>;

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        checkingNumber: 1,
        interestRate: 0,
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
        checkingNumber: null,
        interestRate: 2.5,
        availableBalance: 3000,
        currentBalance: 3000,
        currency: "USD",
        status: "Active"
      }
    ]);

    fetchHistoryMock.mockResolvedValue({
      items: [
        {
          transactionId: "txn-100",
          accountId: "acc-1",
          transactionType: "DEPOSIT",
          bookedAt: "2026-06-29T10:00:00Z",
          description: "Payroll",
          category: "Deposit",
          amount: 500,
          currency: "USD",
          direction: "CREDIT",
          status: "Completed"
        }
      ],
      page: 1,
      pageSize: 10,
      totalItems: 1,
      totalPages: 1
    });

    renderPage();

    expect(await screen.findByText(/Deposit · Payroll/i)).toBeInTheDocument();
    expect(screen.getByText(/Account: Everyday \(acc-1\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Final total account balance/i)).toBeInTheDocument();
    expect(screen.getByText(/\$4,250\.00/i)).toBeInTheDocument();
  });

  it("resolves admin customer scope by name for deposits", async () => {
    (session.getNormalizedTokenRole as jest.MockedFunction<typeof session.getNormalizedTokenRole>).mockReturnValue("ADMIN");

    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchHistoryMock = transactions.fetchTransactionHistory as jest.MockedFunction<typeof transactions.fetchTransactionHistory>;
    const submitDepositMock = transactions.submitDeposit as jest.MockedFunction<typeof transactions.submitDeposit>;

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1250,
        currentBalance: 1250,
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
      reference: "txn-admin-1",
      transactionType: "DEPOSIT",
      status: "Completed",
      submittedAt: "2026-06-26T11:15:00Z",
      postedAmount: 50,
      currency: "USD",
      balanceAfter: 1300
    });

    renderPage();

    fireEvent.change(await screen.findByLabelText(/Target customer name or ID/i), {
      target: { value: "Casey Admin" }
    });

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-200");
    });

    fireEvent.change(screen.getAllByLabelText(/^Account$/i)[0], {
      target: { value: "acc-1" }
    });

    fireEvent.change(screen.getAllByLabelText(/^Amount$/i)[0], {
      target: { value: "50" }
    });

    const depositButton = screen.getByRole("button", { name: /Submit deposit/i });
    await waitFor(() => {
      expect(depositButton).not.toBeDisabled();
    });

    fireEvent.click(depositButton);

    await waitFor(() => {
      expect(submitDepositMock).toHaveBeenCalled();
      const firstCall = submitDepositMock.mock.calls[0]?.[0];
      expect(firstCall?.accountId).toBe("acc-1");
      expect(firstCall?.customerId).toBe("cust-200");
    });
  });

  it("allows admins to switch selected customers from dropdown without clearing search", async () => {
    (session.getNormalizedTokenRole as jest.MockedFunction<typeof session.getNormalizedTokenRole>).mockReturnValue("ADMIN");

    (customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>).mockResolvedValue([
      {
        customerId: "cust-200",
        externalCustomerKey: "ext-200",
        fullName: "Casey Admin",
        email: "casey@example.com",
        mobile: "+61 400 000 200",
        status: "ACTIVE",
        joinedAt: "2024-05-01T00:00:00Z"
      },
      {
        customerId: "cust-201",
        externalCustomerKey: "ext-201",
        fullName: "Casey Delta",
        email: "casey.delta@example.com",
        mobile: "+61 400 000 201",
        status: "ACTIVE",
        joinedAt: "2024-05-02T00:00:00Z"
      }
    ]);

    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchHistoryMock = transactions.fetchTransactionHistory as jest.MockedFunction<typeof transactions.fetchTransactionHistory>;

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1250,
        currentBalance: 1250,
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

    fireEvent.change(await screen.findByLabelText(/Target customer name or ID/i), {
      target: { value: "Casey" }
    });

    const matchingCustomers = screen.getByLabelText(/Matching customers/i);

    fireEvent.change(matchingCustomers, {
      target: { value: "cust-200" }
    });

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-200");
    });

    expect(screen.getByLabelText(/Target customer name or ID/i)).toHaveValue("Casey");

    fireEvent.change(matchingCustomers, {
      target: { value: "cust-201" }
    });

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-201");
    });

    expect(screen.getByLabelText(/Target customer name or ID/i)).toHaveValue("Casey");
  });
});
