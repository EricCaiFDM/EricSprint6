import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AccountManagementPage } from "./AccountManagementPage";
import * as accounts from "../services/accounts";
import * as session from "../services/session";

jest.mock("../services/accounts");
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
});
