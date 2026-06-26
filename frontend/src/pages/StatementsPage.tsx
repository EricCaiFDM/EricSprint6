import { useQuery } from "@tanstack/react-query";
import { fetchStatements } from "../services/statements";
import { formatCurrency, formatDate } from "../utils/formatting";

export function StatementsPage() {
  const statementsQuery = useQuery({
    queryKey: ["statements"],
    queryFn: fetchStatements
  });

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Statements</h2>
          <p className="page-subtitle">Review monthly statements and download official records.</p>
        </div>
      </header>

      <article className="surface-card">
        <ul className="statement-list">
          {(statementsQuery.data ?? []).map((statement) => (
            <li key={statement.statementId} className="statement-item">
              <div>
                <p className="item-title">{statement.periodLabel}</p>
                <p className="item-meta">
                  {statement.accountName} · Issued {formatDate(statement.issuedAt)}
                </p>
              </div>
              <div className="statement-item-right">
                <p className="item-emphasis">
                  {formatCurrency(statement.closingBalance, statement.currency)}
                </p>
                <span className={statement.status === "Ready" ? "status-pill status-pill--ok" : "status-pill"}>
                  {statement.status}
                </span>
                <button type="button" className="button-secondary" disabled={statement.status !== "Ready"}>
                  Download PDF
                </button>
              </div>
            </li>
          ))}
        </ul>
      </article>
    </section>
  );
}
