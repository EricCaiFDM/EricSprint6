import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation, useParams } from "react-router-dom";

import { StatementsPage } from "./StatementsPage";
import * as accounts from "../services/accounts";
import * as customers from "../services/customers";
import * as statements from "../services/statements";

jest.mock("../services/accounts");
jest.mock("../services/customers");
jest.mock("../services/statements");

function createMockJwt(claims: Record<string, string>): string {
  const payload = window
    .btoa(JSON.stringify(claims))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}

function StatementDetailsRouteProbe() {
  const { statementId } = useParams<{ statementId: string }>();
  const location = useLocation();

  return <p>{`statement:${statementId ?? ""}${location.search}`}</p>;
}

describe("StatementsPage", () => {
  function renderPage(initialPath = "/customer/statements") {
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
            <Route path="/customer/statements" element={<StatementsPage />} />
            <Route path="/customer/statements/:statementId" element={<StatementDetailsRouteProbe />} />
            <Route path="/admin/statements" element={<StatementsPage />} />
            <Route path="/admin/statements/:statementId" element={<StatementDetailsRouteProbe />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.clear();

    (accounts.fetchAccounts as jest.MockedFunction<typeof accounts.fetchAccounts>).mockResolvedValue([
      {
        accountId: "acc-1",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 1234",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 1000,
        currentBalance: 1000,
        currency: "USD",
        status: "Active"
      }
    ]);

    (customers.fetchCustomersForAdmin as jest.MockedFunction<typeof customers.fetchCustomersForAdmin>).mockResolvedValue([
      {
        customerId: "cust-1",
        externalCustomerKey: "ext-1",
        fullName: "Chris Admin Scope",
        email: "scope@example.com",
        mobile: "+61 400 000 111",
        status: "ACTIVE",
        joinedAt: "2026-01-01T00:00:00Z"
      }
    ]);
  });

  it("lets admin select a customer scope before loading statements", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;
    fetchStatementsMock.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      totalItems: 0,
      totalPages: 1
    });

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-001",
        email: "admin@example.com",
        role: "ADMIN"
      })
    );

    renderPage("/admin/statements");

    expect(await screen.findByRole("heading", { name: /Admin scope/i })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Target customer name or ID/i), {
      target: { value: "cust-1" }
    });

    await waitFor(() => {
      expect(accounts.fetchAccounts).toHaveBeenCalledWith("cust-1");
    });

    await waitFor(() => {
      expect(fetchStatementsMock).toHaveBeenCalledWith({
        accountId: "acc-1",
        periodYearMonth: undefined,
        page: 1,
        pageSize: 20
      });
    });
  });

  it("navigates to customer statement detail route from view details", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;

    fetchStatementsMock.mockResolvedValue({
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
      pageSize: 20,
      totalItems: 1,
      totalPages: 1
    });

    renderPage("/customer/statements");

    await waitFor(() => {
      expect(fetchStatementsMock).toHaveBeenCalledWith({
        accountId: "acc-1",
        periodYearMonth: undefined,
        page: 1,
        pageSize: 20
      });
    });

    fireEvent.click(await screen.findByRole("button", { name: /View details/i }));

    expect(await screen.findByText("statement:stmt-1")).toBeInTheDocument();
  });

  it("preserves admin customer scope query when navigating to statement detail route", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;

    fetchStatementsMock.mockResolvedValue({
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
      pageSize: 20,
      totalItems: 1,
      totalPages: 1
    });

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-001",
        email: "admin@example.com",
        role: "ADMIN"
      })
    );

    renderPage("/admin/statements?customerId=cust-1");

    await waitFor(() => {
      expect(accounts.fetchAccounts).toHaveBeenCalledWith("cust-1");
      expect(fetchStatementsMock).toHaveBeenCalled();
    });

    fireEvent.click(await screen.findByRole("button", { name: /View details/i }));

    expect(await screen.findByText("statement:stmt-1?customerId=cust-1")).toBeInTheDocument();
  });

  it("submits generation request with selected options", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;
    const generateStatementMock = statements.generateStatement as jest.MockedFunction<typeof statements.generateStatement>;

    fetchStatementsMock.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      totalItems: 0,
      totalPages: 1
    });

    generateStatementMock.mockResolvedValue({
      statementId: "stmt-new",
      generationStatus: "PROCESSING"
    });

    renderPage();

    await waitFor(() => {
      expect(fetchStatementsMock).toHaveBeenCalled();
    });

    const monthInputs = screen.getAllByLabelText(/Generation period/i);
    fireEvent.change(monthInputs[0], { target: { value: "2026-06" } });

    fireEvent.change(screen.getByLabelText(/Generation mode/i), {
      target: { value: "CORRECTION" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Generate statement/i }));

    await waitFor(() => {
      expect(generateStatementMock).toHaveBeenCalled();
    });

    expect(generateStatementMock.mock.calls[0]?.[0]).toEqual({
      accountId: "acc-1",
      periodYearMonth: "2026-06",
      generationMode: "CORRECTION"
    });

    expect(await screen.findByText(/Statement generation processing/i)).toBeInTheDocument();
  });
});
