import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  buildSpendingInsightMonthWindow,
  currentLocalYearMonth,
  fetchSpendingInsights
} from "../services/insights";
import { formatCurrency } from "../utils/formatting";

const PERIOD_YEAR_MONTH_PATTERN = /^(\d{4})-(0[1-9]|1[0-2])$/;

export function SpendingInsightsPage() {
  const currentYearMonth = currentLocalYearMonth();
  const [selectedYearMonth, setSelectedYearMonth] = useState(currentYearMonth);

  const selectedPeriod = useMemo(
    () => buildSpendingInsightMonthWindow(selectedYearMonth),
    [selectedYearMonth]
  );

  const insightsQuery = useQuery({
    queryKey: ["insights", "spending", selectedPeriod.periodStartUtc, selectedPeriod.periodEndUtc],
    queryFn: () =>
      fetchSpendingInsights({
        periodStartUtc: selectedPeriod.periodStartUtc,
        periodEndUtc: selectedPeriod.periodEndUtc
      })
  });

  const insights = insightsQuery.data;
  const isLowConfidence = insights?.confidenceLevel === "LOW";

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Spending insights</h2>
          <p className="page-subtitle">Understand where your money goes and how trends are shifting.</p>
        </div>
        <div className="actions" aria-label="Insight period selector">
          <label>
            Insight month
            <input
              type="month"
              value={selectedYearMonth}
              max={currentYearMonth}
              onChange={(event) => {
                const nextValue = event.target.value;
                if (PERIOD_YEAR_MONTH_PATTERN.test(nextValue)) {
                  setSelectedYearMonth(nextValue);
                }
              }}
            />
          </label>
        </div>
      </header>

      {insightsQuery.isPending ? (
        <article className="surface-card">
          <p className="hint-text">Loading insights...</p>
        </article>
      ) : insightsQuery.isError ? (
        <article className="surface-card">
          <p className="hint-text">Unable to load insights: {(insightsQuery.error as Error).message}</p>
        </article>
      ) : insights ? (
        <section className="two-column-grid">
          <article className="surface-card">
            <h3>{selectedPeriod.monthLabel}</h3>
            <p className="item-meta">Spending period: {selectedPeriod.rangeLabel}</p>
            <p className="summary-value">{formatCurrency(insights.totalSpend, insights.currency)}</p>
            {isLowConfidence ? (
              <p className="hint-text">Insights are warming up as more spending activity is captured.</p>
            ) : (
              <p className="hint-text">{insights.confidenceLabel} · Coverage {insights.coverageRatio.toFixed(2)}%</p>
            )}
            <p className="item-meta">{insights.confidenceReason}</p>
            <p className="item-meta">{insights.methodology}</p>
          </article>

          <article className="surface-card">
            <h3>Category breakdown</h3>
            {insights.categories.length > 0 ? (
              <ul className="insight-list">
                {insights.categories.map((category) => (
                  <li key={category.category} className="insight-item">
                    <div className="insight-row">
                      <p className="item-title">{category.category}</p>
                      <p className="item-emphasis">{formatCurrency(category.amount, insights.currency)}</p>
                    </div>
                    <div className="insight-bar-track">
                      <div
                        className="insight-bar-fill"
                        style={{ width: `${Math.min(100, Math.round(category.ratio * 100))}%` }}
                      />
                    </div>
                    <p className="item-meta">
                      {Math.round(category.ratio * 100)}% of spend · Trend {category.trend}
                    </p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="hint-text">No posted spending transactions were found in this period.</p>
            )}
          </article>
        </section>
      ) : (
        <article className="surface-card">
          <p className="hint-text">No spending insights available.</p>
        </article>
      )}
    </section>
  );
}
