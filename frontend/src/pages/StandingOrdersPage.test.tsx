import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { StandingOrdersPage } from "./StandingOrdersPage";
import * as accountsService from "../services/accounts";
import * as standingOrdersService from "../services/standingOrders";

jest.mock("../services/accounts");
jest.mock("../services/standingOrders");

describe("StandingOrdersPage", () => {
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
          <StandingOrdersPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();

    (accountsService.fetchAccounts as jest.MockedFunction<typeof accountsService.fetchAccounts>).mockResolvedValue([
      {
        accountId: "acc-a",
        accountName: "Everyday",
        accountType: "Everyday",
        accountNumberMasked: "**** 0001",
        checkingNumber: 1,
        interestRate: 0,
        availableBalance: 200,
        currentBalance: 200,
        currency: "USD",
        status: "Active"
      },
      {
        accountId: "acc-b",
        accountName: "Savings",
        accountType: "Savings",
        accountNumberMasked: "**** 0002",
        checkingNumber: null,
        interestRate: 2.5,
        availableBalance: 500,
        currentBalance: 500,
        currency: "USD",
        status: "Active"
      }
    ]);

    (standingOrdersService.fetchStandingOrders as jest.MockedFunction<typeof standingOrdersService.fetchStandingOrders>).mockResolvedValue([
      {
        standingOrderId: "so-1",
        sourceAccountId: "acc-a",
        destinationAccountId: "acc-b",
        amount: 25,
        cadence: "WEEKLY",
        lifecycleState: "ACTIVE",
        nextExecutionAtUtc: "2026-06-30T00:00:00Z",
        effectiveFromUtc: "2026-06-01T00:00:00Z",
        effectiveToUtc: null
      }
    ]);

    (standingOrdersService.createStandingOrder as jest.MockedFunction<typeof standingOrdersService.createStandingOrder>).mockResolvedValue({
      standingOrderId: "so-2",
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: 10,
      cadence: "MONTHLY",
      lifecycleState: "ACTIVE",
      nextExecutionAtUtc: "2026-07-01T00:00:00Z",
      effectiveFromUtc: "2026-06-01T00:00:00Z",
      effectiveToUtc: null
    });

    (standingOrdersService.pauseStandingOrder as jest.MockedFunction<typeof standingOrdersService.pauseStandingOrder>).mockResolvedValue({
      standingOrderId: "so-1",
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: 25,
      cadence: "WEEKLY",
      lifecycleState: "PAUSED",
      nextExecutionAtUtc: null,
      effectiveFromUtc: "2026-06-01T00:00:00Z",
      effectiveToUtc: null
    });

    (standingOrdersService.resumeStandingOrder as jest.MockedFunction<typeof standingOrdersService.resumeStandingOrder>).mockResolvedValue({
      standingOrderId: "so-1",
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: 25,
      cadence: "WEEKLY",
      lifecycleState: "ACTIVE",
      nextExecutionAtUtc: "2026-06-30T00:00:00Z",
      effectiveFromUtc: "2026-06-01T00:00:00Z",
      effectiveToUtc: null
    });

    (standingOrdersService.cancelStandingOrder as jest.MockedFunction<typeof standingOrdersService.cancelStandingOrder>).mockResolvedValue({
      standingOrderId: "so-1",
      sourceAccountId: "acc-a",
      destinationAccountId: "acc-b",
      amount: 25,
      cadence: "WEEKLY",
      lifecycleState: "CANCELLED",
      nextExecutionAtUtc: null,
      effectiveFromUtc: "2026-06-01T00:00:00Z",
      effectiveToUtc: null
    });
  });

  it("creates a standing order with selected values", async () => {
    renderPage();

    expect(await screen.findByText(/Configured standing orders/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Amount/i), { target: { value: "45.50" } });
    fireEvent.change(screen.getByLabelText(/Cadence/i), { target: { value: "MONTHLY" } });
    fireEvent.click(screen.getByRole("button", { name: /Create standing order/i }));

    await waitFor(() => {
      expect(standingOrdersService.createStandingOrder).toHaveBeenCalled();
    });
  });

  it("pauses an active standing order", async () => {
    renderPage();

    expect(await screen.findByText(/Everyday to Savings/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Pause/i }));

    await waitFor(() => {
      expect(standingOrdersService.pauseStandingOrder).toHaveBeenCalledWith("so-1");
    });
  });
});
