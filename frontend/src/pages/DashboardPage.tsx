import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAccounts } from "../services/accounts";
import { fetchRecentTransactions } from "../services/transactions";
import { fetchStandingOrders } from "../services/standingOrders";
import { formatCurrency, formatDate } from "../utils/formatting";

export function DashboardPage() {
  const accountsQuery = useQuery({
    queryKey: ["accounts"],
    queryFn: fetchAccounts
  });
  const transactionsQuery = useQuery({
    queryKey: ["transactions", "recent"],
    queryFn: fetchRecentTransactions
  });
  const standingOrdersQuery = useQuery({
    queryKey: ["standing-orders"],
    queryFn: fetchStandingOrders
  });

  const accounts = accountsQuery.data ?? [];
  const transactions = transactionsQuery.data ?? [];
  const standingOrders = standingOrdersQuery.data ?? [];

  const totalBalance = useMemo(
    () => accounts.reduce((sum, account) => sum + account.availableBalance, 0),
    [accounts]
  );

  const monthlyOutflow = useMemo(
    () => transactions
      .filter((transaction) => transaction.direction === "DEBIT")
      .reduce((sum, transaction) => sum + transaction.amount, 0),
    [transactions]
  );

  const upcomingCount = standingOrders.filter((order) => order.status === "Active").length;

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Dashboard</h2>
          <p className="page-subtitle">
            See balances, recent activity, and upcoming scheduled payments at a glance.
          </p>
        </div>
      </header>

      <section className="summary-grid" aria-label="Account summaries">
        <article className="summary-card">
          <p className="summary-label">Total available balance</p>
          <p className="summary-value">{formatCurrency(totalBalance, "AUD")}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Spending this period</p>
          <p className="summary-value">{formatCurrency(monthlyOutflow, "AUD")}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Active scheduled payments</p>
          <p className="summary-value">{upcomingCount}</p>
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Recent activity</h3>
          <ul className="activity-list">
            {transactions.slice(0, 5).map((transaction) => (
              <li key={transaction.transactionId} className="activity-item">
                <div>
                  <p className="item-title">{transaction.description}</p>
                  <p className="item-meta">{formatDate(transaction.bookedAt)} · {transaction.category}</p>
                </div>
                <p className={transaction.direction === "CREDIT" ? "amount-credit" : "amount-debit"}>
                  {transaction.direction === "CREDIT" ? "+" : "-"}
                  {formatCurrency(transaction.amount, transaction.currency)}
                </p>
              </li>
            ))}
          </ul>
        </article>

        <article className="surface-card">
          <h3>Scheduled payments due soon</h3>
          <ul className="stack-list">
            {standingOrders.slice(0, 4).map((order) => (
              <li key={order.standingOrderId} className="stack-list-item">
                <div>
                  <p className="item-title">{order.payeeName}</p>
                  <p className="item-meta">{order.frequency} · Next run {formatDate(order.nextRunAt)}</p>
                </div>
                <p className="item-emphasis">{formatCurrency(order.amount, order.currency)}</p>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </section>
  );
}
