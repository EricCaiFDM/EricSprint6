import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AccountManagementPage } from "./AccountManagementPage";
import * as accounts from "../services/accounts";
import * as customers from "../services/customers";
import * as session from "../services/session";

jest.mock("../services/accounts");
jest.mock("../services/customers");
jest.mock("../services/session");

describe("AccountManagementPage", () => {
  function renderPage(initialPath = "/customer/accounts") {
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
            <Route path="/customer/accounts" element={<AccountManagementPage />} />
            <Route path="/customer/accounts/:accountId" element={<div>Account details route</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
    const getRoleMock = session.getNormalizedTokenRole as jest.MockedFunction<typeof session.getNormalizedTokenRole>;
    getRoleMock.mockReturnValue("CUSTOMER");
  });

  it("opens account details page when user clicks a listed account", async () => {
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-100",
        accountName: "Daily Spend",
        accountType: "Everyday",
        accountNumberMasked: "**** 0100",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 350.75,
        currentBalance: 350.75,
        currency: "USD",
        status: "Active"
      }
    ]);

    renderPage();

    expect(await screen.findByText(/Daily Spend/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Open details for Daily Spend/i }));

    await waitFor(() => {
      expect(screen.getByText(/Account details route/i)).toBeInTheDocument();
    });
  });

  it("submits savings interest rate when creating savings account", async () => {
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const createCustomerAccountMock = accounts.createCustomerAccount as jest.MockedFunction<typeof accounts.createCustomerAccount>;

    fetchAccountsMock.mockResolvedValue([]);
    createCustomerAccountMock.mockResolvedValue({
      accountId: "acc-200",
      accountName: "Rainy Day",
      accountType: "Savings",
      accountNumberMasked: "**** 0200",
      checkingNumber: null,
      interestRate: 2.75,
      availableBalance: 0,
      currentBalance: 0,
      currency: "USD",
      status: "Active"
    });

    renderPage();

    fireEvent.change(screen.getByLabelText(/Account type/i), {
      target: { value: "SAVINGS" }
    });

    fireEvent.change(screen.getByLabelText(/Interest rate \(%\)/i), {
      target: { value: "2.75" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Create account/i }));

    await waitFor(() => {
      expect(createCustomerAccountMock).toHaveBeenCalled();
      expect(createCustomerAccountMock.mock.calls[0]?.[0]).toEqual(expect.objectContaining({
        accountType: "SAVINGS",
        interestRate: 2.75
      }));
    });
  });

  it("resolves admin scope by customer name for account creation", async () => {
    const getRoleMock = session.getNormalizedTokenRole as jest.MockedFunction<typeof session.getNormalizedTokenRole>;
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchCustomersMock = customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>;
    const createCustomerAccountMock = accounts.createCustomerAccount as jest.MockedFunction<typeof accounts.createCustomerAccount>;

    getRoleMock.mockReturnValue("ADMIN");
    fetchCustomersMock.mockResolvedValue([
      {
        customerId: "cust-44",
        externalCustomerKey: "ext-44",
        fullName: "Taylor Green",
        email: "taylor@example.com",
        mobile: "+61 411 111 111",
        status: "ACTIVE",
        joinedAt: "2026-06-01T00:00:00Z"
      }
    ]);
    fetchAccountsMock.mockResolvedValue([]);
    createCustomerAccountMock.mockResolvedValue({
      accountId: "acc-301",
      accountName: "Daily Spend",
      accountType: "Everyday",
      accountNumberMasked: "**** 0301",
      checkingNumber: 1,
      interestRate: 0,
      availableBalance: 0,
      currentBalance: 0,
      currency: "USD",
      status: "Active"
    });

    renderPage("/customer/accounts");

    fireEvent.change(await screen.findByLabelText(/Target customer name or ID/i), {
      target: { value: "Taylor Green" }
    });

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-44");
    });

    fireEvent.click(screen.getByRole("button", { name: /Create account/i }));

    await waitFor(() => {
      expect(createCustomerAccountMock).toHaveBeenCalled();
      const firstCall = createCustomerAccountMock.mock.calls[0]?.[0];
      expect(firstCall).toEqual(expect.objectContaining({
        customerId: "cust-44"
      }));
    });
  });

  it("allows admins to switch selected customers from dropdown without clearing search", async () => {
    const getRoleMock = session.getNormalizedTokenRole as jest.MockedFunction<typeof session.getNormalizedTokenRole>;
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const fetchCustomersMock = customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>;

    getRoleMock.mockReturnValue("ADMIN");
    fetchCustomersMock.mockResolvedValue([
      {
        customerId: "cust-44",
        externalCustomerKey: "ext-44",
        fullName: "Taylor Green",
        email: "taylor@example.com",
        mobile: "+61 411 111 111",
        status: "ACTIVE",
        joinedAt: "2026-06-01T00:00:00Z"
      },
      {
        customerId: "cust-45",
        externalCustomerKey: "ext-45",
        fullName: "Taylor Brown",
        email: "tbrown@example.com",
        mobile: "+61 422 222 222",
        status: "ACTIVE",
        joinedAt: "2026-06-02T00:00:00Z"
      }
    ]);
    fetchAccountsMock.mockResolvedValue([]);

    renderPage("/customer/accounts");

    fireEvent.change(await screen.findByLabelText(/Target customer name or ID/i), {
      target: { value: "Taylor" }
    });

    const matchingCustomers = screen.getByLabelText(/Matching customers/i);

    fireEvent.change(matchingCustomers, {
      target: { value: "cust-44" }
    });

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-44");
    });

    expect(screen.getByLabelText(/Target customer name or ID/i)).toHaveValue("Taylor");

    fireEvent.change(matchingCustomers, {
      target: { value: "cust-45" }
    });

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-45");
    });

    expect(screen.getByLabelText(/Target customer name or ID/i)).toHaveValue("Taylor");
  });
});
