import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AdminDashboardPage } from "./AdminDashboardPage";
import * as accounts from "../services/accounts";
import * as customers from "../services/customers";
import * as session from "../services/session";

jest.mock("../services/accounts");
jest.mock("../services/customers");
jest.mock("../services/session");

describe("AdminDashboardPage", () => {
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
        <MemoryRouter initialEntries={["/admin/dashboard"]}>
          <Routes>
            <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
            <Route path="/admin/customers/:customerId" element={<div>Admin customer details route</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();

    const getTokenEmailMock = session.getTokenEmail as jest.MockedFunction<typeof session.getTokenEmail>;
    const getTokenSubjectMock = session.getTokenSubject as jest.MockedFunction<typeof session.getTokenSubject>;
    const setActiveCustomerIdMock = session.setActiveCustomerId as jest.MockedFunction<typeof session.setActiveCustomerId>;

    getTokenEmailMock.mockReturnValue("admin@example.com");
    getTokenSubjectMock.mockReturnValue("admin-001");
    setActiveCustomerIdMock.mockImplementation(() => undefined);
  });

  it("lists customers and loads the initially selected customer summary", async () => {
    const fetchCustomersMock = customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>;
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;

    fetchCustomersMock.mockResolvedValue([
      {
        customerId: "cust-1",
        externalCustomerKey: "ext-1",
        fullName: "Casey Admin",
        email: "casey@example.com",
        mobile: "+61 400 000 001",
        status: "ACTIVE",
        joinedAt: "2024-05-01T00:00:00Z"
      },
      {
        customerId: "cust-2",
        externalCustomerKey: "ext-2",
        fullName: "Riley Ops",
        email: "riley@example.com",
        mobile: "+61 400 000 002",
        status: "ACTIVE",
        joinedAt: "2024-05-02T00:00:00Z"
      }
    ]);

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-10",
        accountName: "Daily Spend",
        accountType: "Everyday",
        accountNumberMasked: "**** 1010",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1200,
        currentBalance: 1200,
        currency: "USD",
        status: "Active"
      }
    ]);

    renderPage();

    expect(await screen.findByRole("heading", { name: /All customers/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchCustomersMock).toHaveBeenCalledWith(1, 200);
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-1");
    });

    expect(await screen.findByText(/Selected customer accounts/i)).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /Checking & savings accounts/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /Account details workflow/i })).not.toBeInTheDocument();
  });

  it("opens dedicated customer details page when admin clicks a customer", async () => {
    const fetchCustomersMock = customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>;
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;
    const setActiveCustomerIdMock = session.setActiveCustomerId as jest.MockedFunction<typeof session.setActiveCustomerId>;

    fetchCustomersMock.mockResolvedValue([
      {
        customerId: "cust-1",
        externalCustomerKey: "ext-1",
        fullName: "Casey Admin",
        email: "casey@example.com",
        mobile: "+61 400 000 001",
        status: "ACTIVE",
        joinedAt: "2024-05-01T00:00:00Z"
      },
      {
        customerId: "cust-2",
        externalCustomerKey: "ext-2",
        fullName: "Riley Ops",
        email: "riley@example.com",
        mobile: "+61 400 000 002",
        status: "ACTIVE",
        joinedAt: "2024-05-02T00:00:00Z"
      }
    ]);

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-10",
        accountName: "Daily Spend",
        accountType: "Everyday",
        accountNumberMasked: "**** 1010",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1200,
        currentBalance: 1200,
        currency: "USD",
        status: "Active"
      }
    ]);

    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: /Riley Ops/i }));

    await waitFor(() => {
      expect(screen.getByText(/Admin customer details route/i)).toBeInTheDocument();
      expect(setActiveCustomerIdMock).toHaveBeenLastCalledWith("cust-2");
    });
  });

  it("filters customers by search text", async () => {
    const fetchCustomersMock = customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>;
    const fetchAccountsMock = accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>;

    fetchCustomersMock.mockResolvedValue([
      {
        customerId: "cust-1",
        externalCustomerKey: "ext-1",
        fullName: "Casey Admin",
        email: "casey@example.com",
        mobile: "+61 400 000 001",
        status: "ACTIVE",
        joinedAt: "2024-05-01T00:00:00Z"
      },
      {
        customerId: "cust-2",
        externalCustomerKey: "ext-2",
        fullName: "Riley Ops",
        email: "riley@example.com",
        mobile: "+61 400 000 002",
        status: "ACTIVE",
        joinedAt: "2024-05-02T00:00:00Z"
      }
    ]);

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-10",
        accountName: "Daily Spend",
        accountType: "Everyday",
        accountNumberMasked: "**** 1010",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1200,
        currentBalance: 1200,
        currency: "USD",
        status: "Active"
      }
    ]);

    renderPage();

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-1");
    });

    fireEvent.change(screen.getByLabelText(/Search customers by name or ID/i), {
      target: { value: "Riley" }
    });

    expect(screen.getByRole("button", { name: /Riley Ops/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Casey Admin/i })).not.toBeInTheDocument();
  });
});
