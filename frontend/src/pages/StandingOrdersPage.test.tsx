import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { StandingOrdersPage } from "./StandingOrdersPage";
import * as accountsService from "../services/accounts";
import * as standingOrdersService from "../services/standingOrders";

jest.mock("../services/accounts");
jest.mock("../services/standingOrders");

describe("StandingOrdersPage", () => {
  function toUtcIso(date: string, time: string): string {
    return new Date(`${date}T${time}:00`).toISOString();
  }

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
    await waitFor(() => {
      expect(screen.getAllByRole("option", { name: /Everyday \(\*\*\*\* 0001\) · Balance \$200\.00/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole("option", { name: /Savings \(\*\*\*\* 0002\) · Balance \$500\.00/i }).length).toBeGreaterThan(0);
    });

    fireEvent.change(screen.getByLabelText(/Effective from/i), { target: { value: "2026-07-15" } });
    fireEvent.change(screen.getByLabelText(/Occurs at \(local time\)/i), { target: { value: "14:30" } });
    fireEvent.change(screen.getByLabelText(/Amount/i), { target: { value: "45.50" } });
    fireEvent.change(screen.getByLabelText(/Cadence/i), { target: { value: "MONTHLY" } });
    fireEvent.click(screen.getByRole("button", { name: /Create standing order/i }));

    await waitFor(() => {
      expect(standingOrdersService.createStandingOrder).toHaveBeenCalledWith(
        expect.objectContaining({
          amount: 45.5,
          cadence: "MONTHLY",
          effectiveFromUtc: toUtcIso("2026-07-15", "14:30")
        }),
        expect.anything()
      );
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

  it("hides cancelled standing orders from configured list", async () => {
    (standingOrdersService.fetchStandingOrders as jest.MockedFunction<typeof standingOrdersService.fetchStandingOrders>).mockResolvedValueOnce([
      {
        standingOrderId: "so-cancelled",
        sourceAccountId: "acc-a",
        destinationAccountId: "acc-b",
        amount: 15,
        cadence: "MONTHLY",
        lifecycleState: "CANCELLED",
        nextExecutionAtUtc: null,
        effectiveFromUtc: "2026-06-01T00:00:00Z",
        effectiveToUtc: null
      }
    ]);

    renderPage();

    expect(await screen.findByText(/Configured standing orders/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText(/Everyday to Savings/i)).not.toBeInTheDocument();
      expect(screen.queryByRole("button", { name: /Pause/i })).not.toBeInTheDocument();
      expect(screen.queryByRole("button", { name: /Cancel/i })).not.toBeInTheDocument();
    });
  });

  it("asks for confirmation before cancelling a standing order", async () => {
    renderPage();

    expect(await screen.findByText(/Everyday to Savings/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^Cancel$/i }));

    expect(screen.getByRole("dialog", { name: /Cancel standing order\?/i })).toBeInTheDocument();
    expect(standingOrdersService.cancelStandingOrder).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: /Yes, cancel order/i }));

    await waitFor(() => {
      expect(standingOrdersService.cancelStandingOrder).toHaveBeenCalledWith("so-1");
    });
  });

  it("allows dismissing the cancellation confirmation modal", async () => {
    renderPage();

    expect(await screen.findByText(/Everyday to Savings/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^Cancel$/i }));
    expect(screen.getByRole("dialog", { name: /Cancel standing order\?/i })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Keep order/i }));

    expect(screen.queryByRole("dialog", { name: /Cancel standing order\?/i })).not.toBeInTheDocument();
    expect(standingOrdersService.cancelStandingOrder).not.toHaveBeenCalled();
  });
});
