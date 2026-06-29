import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AccountDetailsPage } from "./AccountDetailsPage";
import * as accounts from "../services/accounts";
import * as customers from "../services/customers";

jest.mock("../services/accounts");
jest.mock("../services/customers");

describe("AccountDetailsPage", () => {
  function renderPage(initialPath = "/customer/accounts/acc-100") {
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
            <Route path="/customer/accounts/:accountId" element={<AccountDetailsPage />} />
            <Route path="/customer/accounts" element={<div>Accounts list route</div>} />
            <Route path="/admin/accounts/:accountId" element={<AccountDetailsPage />} />
            <Route path="/admin/accounts" element={<div>Admin accounts list route</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("shows account details and updates the selected account", async () => {
    const fetchAccountDetailsMock = accounts.fetchAccountDetails as jest.MockedFunction<typeof accounts.fetchAccountDetails>;
    const updateAccountMock = accounts.updateCustomerAccount as jest.MockedFunction<typeof accounts.updateCustomerAccount>;

    fetchAccountDetailsMock.mockResolvedValue({
      accountId: "acc-100",
      accountName: "Daily Spend",
      accountType: "Everyday",
      accountNumberMasked: "**** 0100",
      availableBalance: 350.75,
      currentBalance: 350.75,
      currency: "USD",
      status: "Active"
    });

    updateAccountMock.mockResolvedValue({
      accountId: "acc-100",
      accountName: "Emergency Fund",
      accountType: "Everyday",
      accountNumberMasked: "**** 0100",
      availableBalance: 350.75,
      currentBalance: 350.75,
      currency: "USD",
      status: "Paused"
    });

    renderPage();

    expect(await screen.findByText(/Daily Spend/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Nickname/i), {
      target: { value: "Emergency Fund" }
    });

    fireEvent.change(screen.getByLabelText(/Status/i), {
      target: { value: "SUSPENDED" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Update account/i }));

    await waitFor(() => {
      expect(updateAccountMock).toHaveBeenCalled();
    });

    expect(await screen.findByText(/Account updated:/i)).toBeInTheDocument();
  });

  it("returns to account list when clicking back", async () => {
    const fetchAccountDetailsMock = accounts.fetchAccountDetails as jest.MockedFunction<typeof accounts.fetchAccountDetails>;

    fetchAccountDetailsMock.mockResolvedValue({
      accountId: "acc-100",
      accountName: "Daily Spend",
      accountType: "Everyday",
      accountNumberMasked: "**** 0100",
      availableBalance: 350.75,
      currentBalance: 350.75,
      currency: "USD",
      status: "Active"
    });

    renderPage();

    await screen.findByText(/Daily Spend/i);

    fireEvent.click(screen.getByRole("button", { name: /Back to accounts/i }));

    await waitFor(() => {
      expect(screen.getByText(/Accounts list route/i)).toBeInTheDocument();
    });
  });

  it("shows confirmation modal before deleting account for customer route", async () => {
    const fetchAccountDetailsMock = accounts.fetchAccountDetails as jest.MockedFunction<typeof accounts.fetchAccountDetails>;
    const deleteAccountMock = accounts.deleteCustomerAccount as jest.MockedFunction<typeof accounts.deleteCustomerAccount>;

    fetchAccountDetailsMock.mockResolvedValue({
      accountId: "acc-100",
      accountName: "Daily Spend",
      accountType: "Everyday",
      accountNumberMasked: "**** 0100",
      availableBalance: 350.75,
      currentBalance: 350.75,
      currency: "USD",
      status: "Active"
    });

    deleteAccountMock.mockResolvedValue({
      status: "DELETED",
      message: "Account deleted"
    });

    renderPage();

    await screen.findByText(/Daily Spend/i);

    fireEvent.click(screen.getByRole("button", { name: /Delete account/i }));

    expect(screen.getByRole("dialog", { name: /Confirm account deletion/i })).toBeInTheDocument();
    expect(deleteAccountMock).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: /Cancel/i }));

    expect(screen.queryByRole("dialog", { name: /Confirm account deletion/i })).not.toBeInTheDocument();
    expect(deleteAccountMock).not.toHaveBeenCalled();
  });

  it("deletes account after confirmation and redirects to customer accounts list", async () => {
    const fetchAccountDetailsMock = accounts.fetchAccountDetails as jest.MockedFunction<typeof accounts.fetchAccountDetails>;
    const deleteAccountMock = accounts.deleteCustomerAccount as jest.MockedFunction<typeof accounts.deleteCustomerAccount>;

    fetchAccountDetailsMock.mockResolvedValue({
      accountId: "acc-100",
      accountName: "Daily Spend",
      accountType: "Everyday",
      accountNumberMasked: "**** 0100",
      availableBalance: 350.75,
      currentBalance: 350.75,
      currency: "USD",
      status: "Active"
    });

    deleteAccountMock.mockResolvedValue({
      status: "DELETED",
      message: "Account deleted"
    });

    renderPage();

    await screen.findByText(/Daily Spend/i);

    fireEvent.click(screen.getByRole("button", { name: /Delete account/i }));
    fireEvent.click(screen.getByRole("button", { name: /Yes, delete this account/i }));

    await waitFor(() => {
      expect(deleteAccountMock).toHaveBeenCalled();
      expect(deleteAccountMock.mock.calls[0][0]).toBe("acc-100");
      expect(screen.getByText(/Accounts list route/i)).toBeInTheDocument();
    });
  });

  it("shows associated customer email on admin account details route", async () => {
    const fetchAccountDetailsMock = accounts.fetchAccountDetails as jest.MockedFunction<typeof accounts.fetchAccountDetails>;
    const fetchCustomerDetailsMock = customers.fetchCustomerDetails as jest.MockedFunction<typeof customers.fetchCustomerDetails>;

    fetchAccountDetailsMock.mockResolvedValue({
      accountId: "acc-500",
      accountName: "House Savings",
      accountType: "Savings",
      accountNumberMasked: "**** 0500",
      availableBalance: 9200,
      currentBalance: 9200,
      currency: "USD",
      status: "Active"
    });

    fetchCustomerDetailsMock.mockResolvedValue({
      customerId: "cust-500",
      fullName: "Riley Ops",
      email: "riley.ops@example.com",
      mobile: "+61 400 000 002",
      status: "ACTIVE",
      joinedAt: "2024-05-11T00:00:00Z"
    });

    renderPage("/admin/accounts/acc-500?customerId=cust-500");

    expect(await screen.findByText(/House Savings/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchCustomerDetailsMock).toHaveBeenCalledWith("cust-500");
    });

    expect(await screen.findByText("riley.ops@example.com")).toBeInTheDocument();
  });
});
