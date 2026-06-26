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
            <Route path="/admin/accounts/:accountId" element={<div>Admin account details route</div>} />
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

  it("lists customers and loads clicked customer accounts", async () => {
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

    fetchAccountsMock.mockImplementation(async (customerId?: string) => {
      if (customerId === "cust-2") {
        return [
          {
            accountId: "acc-20",
            accountName: "House Savings",
            accountType: "Savings",
            accountNumberMasked: "**** 2020",
            availableBalance: 4100,
            currentBalance: 4100,
            currency: "USD",
            status: "Active"
          }
        ];
      }

      return [
        {
          accountId: "acc-10",
          accountName: "Daily Spend",
          accountType: "Everyday",
          accountNumberMasked: "**** 1010",
          availableBalance: 1200,
          currentBalance: 1200,
          currency: "USD",
          status: "Active"
        }
      ];
    });

    renderPage();

    expect(await screen.findByRole("heading", { name: /All customers/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchCustomersMock).toHaveBeenCalledWith(1, 200);
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-1");
    });

    fireEvent.click(screen.getByRole("button", { name: /Riley Ops/i }));

    await waitFor(() => {
      expect(fetchAccountsMock).toHaveBeenCalledWith("cust-2");
    });

    expect(await screen.findByText(/House Savings/i)).toBeInTheDocument();
  });

  it("opens dedicated account details page when admin clicks an account", async () => {
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
      }
    ]);

    fetchAccountsMock.mockResolvedValue([
      {
        accountId: "acc-10",
        accountName: "Daily Spend",
        accountType: "Everyday",
        accountNumberMasked: "**** 1010",
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

    fireEvent.click(await screen.findByRole("button", { name: /Daily Spend/i }));

    await waitFor(() => {
      expect(screen.getByText(/Admin account details route/i)).toBeInTheDocument();
    });
  });
});
