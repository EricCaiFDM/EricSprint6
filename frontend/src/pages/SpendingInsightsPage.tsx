import { useQuery } from "@tanstack/react-query";
import { fetchSpendingInsights } from "../services/insights";
import { formatCurrency } from "../utils/formatting";

export function SpendingInsightsPage() {
  const insightsQuery = useQuery({
    queryKey: ["insights", "spending"],
    queryFn: fetchSpendingInsights
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
      </header>

      {insights ? (
        <section className="two-column-grid">
          <article className="surface-card">
            <h3>{insights.periodLabel}</h3>
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
          <p className="hint-text">Loading insights...</p>
        </article>
      )}
    </section>
  );
}
