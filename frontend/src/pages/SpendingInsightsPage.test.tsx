import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { SpendingInsightsPage } from "./SpendingInsightsPage";
import * as insights from "../services/insights";

jest.mock("../services/insights", () => {
  const actual = jest.requireActual("../services/insights");
  return {
    ...actual,
    fetchSpendingInsights: jest.fn()
  };
});

function expectedMonthQuery(periodYearMonth: string): { periodStartUtc: string; periodEndUtc: string } {
  const [yearPart, monthPart] = periodYearMonth.split("-");
  const year = Number(yearPart);
  const monthIndex = Number(monthPart) - 1;

  const startLocal = new Date(year, monthIndex, 1, 0, 0, 0, 0);
  const endExclusiveLocal = new Date(year, monthIndex + 1, 1, 0, 0, 0, 0);

  return {
    periodStartUtc: startLocal.toISOString(),
    periodEndUtc: endExclusiveLocal.toISOString()
  };
}

describe("SpendingInsightsPage", () => {
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
          <SpendingInsightsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    jest.setSystemTime(new Date("2026-07-15T12:00:00.000Z"));

    (insights.fetchSpendingInsights as jest.MockedFunction<typeof insights.fetchSpendingInsights>).mockResolvedValue({
      periodLabel: "July 2026",
      periodStartUtc: "2026-07-01T00:00:00Z",
      periodEndUtc: "2026-08-01T00:00:00Z",
      scopeType: "CUSTOMER",
      scopeId: "cust-100",
      totalSpend: 500.25,
      currency: "AUD",
      confidenceLabel: "Medium confidence",
      confidenceLevel: "MEDIUM",
      coverageRatio: 82.5,
      confidenceReason: "Insights are based on an adequate sample size.",
      status: "GENERATED",
      methodology: "Spending insights use posted debit transactions and approved taxonomy mappings.",
      categories: [
        {
          category: "Groceries",
          amount: 210,
          ratio: 0.42,
          trend: "up"
        }
      ]
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("queries insights with current local month boundaries by default", async () => {
    renderPage();

    await waitFor(() => {
      expect(insights.fetchSpendingInsights).toHaveBeenCalledTimes(1);
      expect(insights.fetchSpendingInsights).toHaveBeenCalledWith(expectedMonthQuery("2026-07"));
    });

    expect(screen.getByLabelText(/Insight month/i)).toHaveValue("2026-07");
    expect(await screen.findByText(/Spending period:/i)).toBeInTheDocument();
    expect(screen.getByText(/Category breakdown/i)).toBeInTheDocument();
  });

  it("allows choosing a specific month for insights", async () => {
    renderPage();

    await waitFor(() => {
      expect(insights.fetchSpendingInsights).toHaveBeenCalledWith(expectedMonthQuery("2026-07"));
    });

    fireEvent.change(screen.getByLabelText(/Insight month/i), {
      target: { value: "2026-05" }
    });

    await waitFor(() => {
      expect(insights.fetchSpendingInsights).toHaveBeenCalledTimes(2);
      expect(insights.fetchSpendingInsights).toHaveBeenLastCalledWith(expectedMonthQuery("2026-05"));
    });
  });
});
