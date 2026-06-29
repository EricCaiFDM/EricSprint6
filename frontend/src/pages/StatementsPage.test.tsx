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
  });

  it("loads statement list and retrieves details", async () => {
    const fetchStatementsMock = statements.fetchStatements as jest.MockedFunction<typeof statements.fetchStatements>;
    const fetchStatementMock = statements.fetchStatement as jest.MockedFunction<typeof statements.fetchStatement>;

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

    await waitFor(() => {
      expect(fetchStatementMock).toHaveBeenCalledWith("stmt-1");
    });

    expect(await screen.findByText(/\$125\.00/i)).toBeInTheDocument();
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
});
