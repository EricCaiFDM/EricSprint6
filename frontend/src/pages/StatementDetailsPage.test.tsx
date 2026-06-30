import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import { StatementDetailsPage } from "./StatementDetailsPage";
import * as statements from "../services/statements";
import { formatDate } from "../utils/formatting";

jest.mock("../services/statements");

function createMockJwt(claims: Record<string, string>): string {
  const payload = window
    .btoa(JSON.stringify(claims))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}

describe("StatementDetailsPage", () => {
  function renderPage(initialPath = "/customer/statements/stmt-1") {
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
            <Route path="/customer/statements/:statementId" element={<StatementDetailsPage />} />
            <Route path="/admin/statements/:statementId" element={<StatementDetailsPage />} />
            <Route path="/customer/statements" element={<p>customer statements route</p>} />
            <Route path="/admin/statements" element={<p>admin statements route</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.clear();

    (statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>).mockResolvedValue({
      statementId: "stmt-1",
      accountId: "acc-1",
      periodYearMonth: "2026-06",
      artifactVersion: 1,
      openingBalance: 0,
      closingBalance: 125,
      currencyCode: "USD",
      status: "GENERATED",
      artifactUri: "/statements/stmt-1/artifact/v1.pdf",
      generatedAtUtc: "2026-06-30T00:05:00Z"
    });

    (statements.fetchStatementTransactions as jest.MockedFunction<typeof statements.fetchStatementTransactions>).mockResolvedValue([
      {
        transactionId: "txn-1",
        transactionType: "DEPOSIT",
        bookedAt: "2026-06-10T12:00:00Z",
        description: "Deposit",
        category: "Deposit",
        amount: 125,
        currency: "USD",
        direction: "CREDIT",
        status: "Completed"
      },
      {
        transactionId: "txn-2",
        transactionType: "TRANSFER_DEBIT",
        bookedAt: "2026-06-11T12:00:00Z",
        description: "Transfer sent",
        category: "Transfer",
        amount: 30,
        currency: "USD",
        direction: "DEBIT",
        status: "Completed"
      }
    ]);
  });

  it("loads statement detail and renders transaction ledger table", async () => {
    renderPage();

    await waitFor(() => {
      expect(statements.fetchStatement).toHaveBeenCalledWith("stmt-1");
    });

    await waitFor(() => {
      expect(statements.fetchStatementTransactions).toHaveBeenCalledWith({
        accountId: "acc-1",
        periodYearMonth: "2026-06"
      });
    });

    const table = await screen.findByRole("table", { name: /Statement transactions table/i });
    expect(within(table).getByRole("columnheader", { name: /^Date$/i })).toBeInTheDocument();
    expect(within(table).getByText(/Opening balance/i)).toBeInTheDocument();
    expect(within(table).getByText(/Ref txn-1/i)).toBeInTheDocument();
    expect(within(table).getByText(/Transfer sent/i)).toBeInTheDocument();
    expect(within(table).getByText(/Closing balance/i)).toBeInTheDocument();
  });

  it("renders transaction posting dates using user-local timezone", async () => {
    (statements.fetchStatementTransactions as jest.MockedFunction<typeof statements.fetchStatementTransactions>).mockResolvedValue([
      {
        transactionId: "txn-utc-boundary",
        transactionType: "DEPOSIT",
        bookedAt: "2026-06-30T23:30:00Z",
        description: "Late June deposit",
        category: "Deposit",
        amount: 15,
        currency: "USD",
        direction: "CREDIT",
        status: "Completed"
      }
    ]);

    renderPage();

    const expectedLocalDate = formatDate("2026-06-30T23:30:00Z");
    expect(await screen.findByText(expectedLocalDate)).toBeInTheDocument();
    expect(screen.queryByText(/Date \(UTC\)/i)).not.toBeInTheDocument();
  });

  it("derives opening balance from first local-month transaction balance", async () => {
    (statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>).mockResolvedValue({
      statementId: "stmt-1",
      accountId: "acc-1",
      periodYearMonth: "2026-06",
      artifactVersion: 1,
      openingBalance: 0,
      closingBalance: 0,
      currencyCode: "USD",
      status: "GENERATED",
      artifactUri: "/statements/stmt-1/artifact/v1.pdf",
      generatedAtUtc: "2026-06-30T00:05:00Z"
    });

    (statements.fetchStatementTransactions as jest.MockedFunction<typeof statements.fetchStatementTransactions>).mockResolvedValue([
      {
        transactionId: "txn-first",
        transactionType: "DEPOSIT",
        bookedAt: "2026-06-03T09:00:00Z",
        balanceAfter: 125,
        description: "Deposit",
        category: "Deposit",
        amount: 25,
        currency: "USD",
        direction: "CREDIT",
        status: "Completed"
      },
      {
        transactionId: "txn-second",
        transactionType: "WITHDRAWAL",
        bookedAt: "2026-06-04T09:00:00Z",
        balanceAfter: 100,
        description: "Withdrawal",
        category: "Withdrawal",
        amount: 25,
        currency: "USD",
        direction: "DEBIT",
        status: "Completed"
      }
    ]);

    renderPage();

    const table = await screen.findByRole("table", { name: /Statement transactions table/i });
    const openingRow = within(table).getByText("Opening balance").closest("tr");
    const closingRow = within(table).getByText("Closing balance").closest("tr");

    expect(openingRow).not.toBeNull();
    expect(closingRow).not.toBeNull();
    expect(within(openingRow as HTMLElement).getByText("$100.00")).toBeInTheDocument();
    expect(within(closingRow as HTMLElement).getByText("$100.00")).toBeInTheDocument();
  });

  it("shows a friendly message when statement period has no transactions", async () => {
    (statements.fetchStatementTransactions as jest.MockedFunction<typeof statements.fetchStatementTransactions>).mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText(/No transactions were posted in this statement period/i)).toBeInTheDocument();
  });

  it("downloads statement PDF from details page", async () => {
    const fetchStatementPdfMock = statements.fetchStatementPdf as jest.MockedFunction<typeof statements.fetchStatementPdf>;
    fetchStatementPdfMock.mockResolvedValue({
      blob: new Blob(["%PDF-1.4"], { type: "application/pdf" }),
      fileName: "statement-2026-06-v1.pdf"
    });

    const createObjectURLSpy = jest.fn(() => "blob:test-url");
    const revokeObjectURLSpy = jest.fn();
    const originalCreateObjectURL = URL.createObjectURL;
    const originalRevokeObjectURL = URL.revokeObjectURL;
    Object.defineProperty(URL, "createObjectURL", { value: createObjectURLSpy, configurable: true });
    Object.defineProperty(URL, "revokeObjectURL", { value: revokeObjectURLSpy, configurable: true });

    const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);

    try {
      renderPage();

      fireEvent.click(await screen.findByRole("button", { name: /Download PDF/i }));

      await waitFor(() => {
        expect(fetchStatementPdfMock).toHaveBeenCalledWith(expect.objectContaining({
          statementId: "stmt-1",
          artifactVersion: 1
        }));
      });

      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(await screen.findByText(/Statement PDF download started/i)).toBeInTheDocument();
    } finally {
      clickSpy.mockRestore();
      Object.defineProperty(URL, "createObjectURL", { value: originalCreateObjectURL, configurable: true });
      Object.defineProperty(URL, "revokeObjectURL", { value: originalRevokeObjectURL, configurable: true });
    }
  });

  it("keeps admin customer scope when linking back to statements list", async () => {
    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-001",
        email: "admin@example.com",
        role: "ADMIN"
      })
    );

    renderPage("/admin/statements/stmt-1?customerId=cust-1");

    const backLink = await screen.findByRole("link", { name: /Back to statements/i });
    expect(backLink).toHaveAttribute("href", "/admin/statements?customerId=cust-1");
  });
});
