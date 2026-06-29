import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { StatementsPage } from "./StatementsPage";
import * as accounts from "../services/accounts";
import * as statements from "../services/statements";

jest.mock("../services/accounts");
jest.mock("../services/statements");

describe("StatementsPage", () => {
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
          <StatementsPage />
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
        availableBalance: 1000,
        currentBalance: 1000,
        currency: "USD",
        status: "Active"
      }
    ]);

    (statements.fetchStatementTransactions as jest.MockedFunction<typeof statements.fetchStatementTransactions>).mockResolvedValue([]);
  });

  it("loads statement list, retrieves details, and renders statement transactions", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;
    const fetchStatementMock = statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>;
    const fetchStatementTransactionsMock = statements.fetchStatementTransactions as jest.MockedFunction<
      typeof statements.fetchStatementTransactions
    >;

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

    fetchStatementMock.mockResolvedValue({
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

    fetchStatementTransactionsMock.mockResolvedValue([
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

    renderPage();

    await waitFor(() => {
      expect(fetchStatementsMock).toHaveBeenCalledWith({
        accountId: "acc-1",
        periodYearMonth: undefined,
        page: 1,
        pageSize: 20
      });
    });

    expect(await screen.findByRole("button", { name: /View details/i })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /View details/i }));

    expect(screen.getByText(/Retrieving selected statement details/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchStatementMock).toHaveBeenCalledWith("stmt-1");
    });

    await waitFor(() => {
      expect(fetchStatementTransactionsMock).toHaveBeenCalledWith({
        accountId: "acc-1",
        periodYearMonth: "2026-06"
      });
    });

    expect(await screen.findByText(/\$125\.00/i)).toBeInTheDocument();
    expect(await screen.findByText(/Deposit · Deposit/i)).toBeInTheDocument();
    expect(await screen.findByText(/Transfer \/ standing order debit · Transfer sent/i)).toBeInTheDocument();
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

    (statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>).mockResolvedValue({
      statementId: "stmt-new",
      accountId: "acc-1",
      periodYearMonth: "2026-06",
      artifactVersion: 1,
      openingBalance: 0,
      closingBalance: 0,
      currencyCode: "USD",
      status: "GENERATED",
      artifactUri: "/statements/stmt-new/artifact/v1.pdf",
      generatedAtUtc: "2026-07-01T00:05:00Z"
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

  it("shows a friendly message when statement period has no transactions", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;
    const fetchStatementMock = statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>;
    const fetchStatementTransactionsMock = statements.fetchStatementTransactions as jest.MockedFunction<
      typeof statements.fetchStatementTransactions
    >;

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

    fetchStatementMock.mockResolvedValue({
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

    fetchStatementTransactionsMock.mockResolvedValue([]);

    renderPage();

    await waitFor(() => {
      expect(fetchStatementsMock).toHaveBeenCalled();
    });

    fireEvent.click(await screen.findByRole("button", { name: /View details/i }));

    await waitFor(() => {
      expect(fetchStatementMock).toHaveBeenCalledWith("stmt-1");
    });

    await waitFor(() => {
      expect(fetchStatementTransactionsMock).toHaveBeenCalledWith({
        accountId: "acc-1",
        periodYearMonth: "2026-06"
      });
    });

    expect(await screen.findByText(/No transactions were posted in this statement period/i)).toBeInTheDocument();
  });

  it("downloads selected statement as PDF from details panel", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;
    const fetchStatementMock = statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>;
    const fetchStatementTransactionsMock = statements.fetchStatementTransactions as jest.MockedFunction<
      typeof statements.fetchStatementTransactions
    >;
    const fetchStatementPdfMock = statements.fetchStatementPdf as jest.MockedFunction<typeof statements.fetchStatementPdf>;

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

    fetchStatementMock.mockResolvedValue({
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

    fetchStatementTransactionsMock.mockResolvedValue([]);
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

      await waitFor(() => {
        expect(fetchStatementsMock).toHaveBeenCalled();
      });

      fireEvent.click(await screen.findByRole("button", { name: /View details/i }));

      await waitFor(() => {
        expect(fetchStatementMock).toHaveBeenCalledWith("stmt-1");
      });

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
});
