import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";

import { fetchAccounts, type BankAccount } from "../services/accounts";
import { fetchCustomerDetails } from "../services/customers";
import { fetchSpendingInsights } from "../services/insights";
import { fetchStatements, type StatementListItem } from "../services/statements";
import { fetchTransactionHistory } from "../services/transactions";
import { formatCurrency, formatDate, formatDateTime, formatStatementPeriod } from "../utils/formatting";

type AccountStatements = {
  accountId: string;
  accountName: string;
  accountNumberMasked: string;
  statements: StatementListItem[];
};

const STATEMENTS_PAGE_SIZE = 6;
const TRANSACTIONS_PAGE_SIZE = 20;

export function AdminCustomerDetailsPage() {
  const { customerId: routeCustomerId } = useParams<{ customerId: string }>();
  const customerId = routeCustomerId?.trim() ?? "";

  const [selectedAccountId, setSelectedAccountId] = useState("");

  const customerQuery = useQuery({
    queryKey: ["admin", "customers", "details", customerId],
    queryFn: () => fetchCustomerDetails(customerId),
    enabled: Boolean(customerId)
  });

  const accountsQuery = useQuery({
    queryKey: ["admin", "customers", customerId, "accounts"],
    queryFn: () => fetchAccounts(customerId),
    enabled: Boolean(customerId)
  });

  const insightsQuery = useQuery({
    queryKey: ["admin", "customers", customerId, "insights"],
    queryFn: () =>
      fetchSpendingInsights({
        scopeType: "CUSTOMER",
        scopeId: customerId
      }),
    enabled: Boolean(customerId)
  });

  const accounts = accountsQuery.data ?? [];

  const statementsQuery = useQuery({
    queryKey: ["admin", "customers", customerId, "statements", accounts.map((account) => account.accountId).join("|")],
    queryFn: async () => {
      const statementsByAccount = await Promise.all(
        accounts.map(async (account) => {
          const response = await fetchStatements({
            accountId: account.accountId,
            page: 1,
            pageSize: STATEMENTS_PAGE_SIZE
          });

          return {
            accountId: account.accountId,
            accountName: account.accountName,
            accountNumberMasked: account.accountNumberMasked,
            statements: response.items
          } satisfies AccountStatements;
        })
      );

      return statementsByAccount;
    },
    enabled: accounts.length > 0
  });

  const selectedAccount = useMemo(() => {
    if (!accounts.length) {
      return null;
    }
    if (selectedAccountId) {
      return accounts.find((account) => account.accountId === selectedAccountId) ?? accounts[0];
    }
    return accounts[0];
  }, [accounts, selectedAccountId]);

  const transactionsQuery = useQuery({
    queryKey: ["admin", "customers", customerId, "transactions", selectedAccount?.accountId ?? ""],
    queryFn: () =>
      fetchTransactionHistory({
        scopeType: "ACCOUNT",
        scopeId: selectedAccount?.accountId,
        page: 1,
        pageSize: TRANSACTIONS_PAGE_SIZE
      }),
    enabled: Boolean(selectedAccount?.accountId)
  });

  const accountStatementGroups = statementsQuery.data ?? [];
  const totalBalance = accounts.reduce((sum, account) => sum + account.currentBalance, 0);
  const recentTransactions = transactionsQuery.data?.items ?? [];

  if (!customerId) {
    return (
      <section className="bank-page">
        <header className="page-header">
          <div>
            <h2 className="page-title">Customer profile overview</h2>
            <p className="page-subtitle">Select a valid customer from the admin dashboard.</p>
          </div>
        </header>
        <article className="surface-card">
          <p className="hint-text">No customer ID was provided in the route.</p>
          <div className="actions">
            <Link className="admin-customer-header-link" to="/admin/dashboard">
              Back to admin dashboard
            </Link>
          </div>
        </article>
      </section>
    );
  }

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Customer profile overview</h2>
          <p className="page-subtitle">Review profile, accounts, transactions, monthly statements, and spending insights in one place.</p>
        </div>
        <div className="actions admin-customer-header-actions">
          <Link
            className="admin-customer-header-link admin-customer-header-link--primary"
            to={`/admin/statements?customerId=${encodeURIComponent(customerId)}`}
          >
            Open statements workspace
          </Link>
          <Link className="admin-customer-header-link" to="/admin/dashboard">
            Back to admin dashboard
          </Link>
        </div>
      </header>

      <section className="summary-grid">
        <article className="summary-card">
          <p className="summary-label">Accounts</p>
          <p className="summary-value">{accounts.length}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Total balance</p>
          <p className="summary-value">
            {formatCurrency(totalBalance, selectedAccount?.currency ?? "USD")}
          </p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Recent transactions</p>
          <p className="summary-value">{recentTransactions.length}</p>
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Customer profile</h3>
          {customerQuery.isPending ? <p className="hint-text">Loading customer profile...</p> : null}
          {customerQuery.isError ? (
            <p className="hint-text">Unable to load customer profile: {(customerQuery.error as Error).message}</p>
          ) : null}
          {customerQuery.data ? (
            <dl className="profile-grid">
              <div>
                <dt>Customer ID</dt>
                <dd>{customerQuery.data.customerId}</dd>
              </div>
              <div>
                <dt>Legal name</dt>
                <dd>{customerQuery.data.fullName}</dd>
              </div>
              <div>
                <dt>Primary email</dt>
                <dd>{customerQuery.data.email || "Not provided"}</dd>
              </div>
              <div>
                <dt>Phone number</dt>
                <dd>{customerQuery.data.mobile || "Not provided"}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{customerQuery.data.status}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{formatDate(customerQuery.data.joinedAt)}</dd>
              </div>
            </dl>
          ) : null}
        </article>

        <article className="surface-card">
          <h3>Spending insights</h3>
          {insightsQuery.isPending ? <p className="hint-text">Loading spending insights...</p> : null}
          {insightsQuery.isError ? (
            <p className="hint-text">Unable to load spending insights: {(insightsQuery.error as Error).message}</p>
          ) : null}
          {insightsQuery.data ? (
            <>
              <p className="item-title">{insightsQuery.data.periodLabel}</p>
              <p className="summary-value">{formatCurrency(insightsQuery.data.totalSpend, insightsQuery.data.currency)}</p>
              {insightsQuery.data.confidenceLevel === "LOW" ? (
                <p className="hint-text">Insights are warming up as more spending activity is captured.</p>
              ) : (
                <p className="hint-text">{insightsQuery.data.confidenceLabel}</p>
              )}
              <p className="item-meta">{insightsQuery.data.confidenceReason}</p>
              <p className="item-meta">{insightsQuery.data.methodology}</p>

              {insightsQuery.data.categories.length > 0 ? (
                <ul className="stack-list">
                  {insightsQuery.data.categories.map((category) => (
                    <li key={category.category} className="stack-list-item">
                      <div className="insight-row">
                        <p className="item-title">{category.category}</p>
                        <p className="item-emphasis">{formatCurrency(category.amount, insightsQuery.data.currency)}</p>
                      </div>
                      <p className="stack-list-meta">{Math.round(category.ratio * 100)}% of spending</p>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="hint-text">No categorized spending was found in this period.</p>
              )}
            </>
          ) : null}
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Accounts</h3>
          {accountsQuery.isPending ? <p className="hint-text">Loading accounts...</p> : null}
          {accountsQuery.isError ? (
            <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
          ) : null}
          {!accountsQuery.isPending && !accountsQuery.isError && accounts.length === 0 ? (
            <p className="hint-text">No accounts are linked to this customer yet.</p>
          ) : null}

          {accounts.length > 0 ? (
            <>
              <label>
                Focus account
                <select value={selectedAccount?.accountId ?? ""} onChange={(event) => setSelectedAccountId(event.target.value)}>
                  {accounts.map((account) => (
                    <option key={account.accountId} value={account.accountId}>
                      {account.accountName} ({account.accountNumberMasked})
                    </option>
                  ))}
                </select>
              </label>

              <ul className="stack-list">
                {accounts.map((account) => (
                  <li key={account.accountId} className="stack-list-item">
                    <p className="item-title">{account.accountName}</p>
                    <p className="stack-list-meta">{describeAccount(account)}</p>
                    <p className="item-emphasis">{formatCurrency(account.currentBalance, account.currency)}</p>
                  </li>
                ))}
              </ul>
            </>
          ) : null}
        </article>

        <article className="surface-card">
          <h3>Transaction history</h3>
          {transactionsQuery.isPending ? <p className="hint-text">Loading transaction history...</p> : null}
          {transactionsQuery.isError ? (
            <p className="hint-text">Unable to load transaction history: {(transactionsQuery.error as Error).message}</p>
          ) : null}
          {!transactionsQuery.isPending && !transactionsQuery.isError && selectedAccount && recentTransactions.length === 0 ? (
            <p className="hint-text">No recent transactions were found for the selected account.</p>
          ) : null}

          {recentTransactions.length > 0 ? (
            <ul className="stack-list">
              {recentTransactions.map((transaction) => (
                <li key={transaction.transactionId} className="stack-list-item">
                  <p className="item-title">{transaction.description || transaction.transactionType}</p>
                  <p className="stack-list-meta">{formatDateTime(transaction.bookedAt)} · {transaction.transactionType}</p>
                  <p className="item-emphasis">
                    {transaction.direction === "DEBIT" ? "-" : ""}
                    {formatCurrency(transaction.amount, transaction.currency)}
                  </p>
                </li>
              ))}
            </ul>
          ) : null}
        </article>
      </section>

      <article className="surface-card">
        <h3>Monthly statements</h3>
        {statementsQuery.isPending ? <p className="hint-text">Loading statements...</p> : null}
        {statementsQuery.isError ? (
          <p className="hint-text">Unable to load statements: {(statementsQuery.error as Error).message}</p>
        ) : null}
        {!statementsQuery.isPending && !statementsQuery.isError && accountStatementGroups.length === 0 ? (
          <p className="hint-text">Statements will appear once monthly statement generation has run for this customer accounts.</p>
        ) : null}

        {accountStatementGroups.length > 0 ? (
          <div className="two-column-grid">
            {accountStatementGroups.map((group) => (
              <article key={group.accountId} className="surface-card">
                <h4>{group.accountName}</h4>
                <p className="item-meta">{group.accountNumberMasked}</p>
                {group.statements.length === 0 ? (
                  <p className="hint-text">No statements found for this account.</p>
                ) : (
                  <ul className="statement-list">
                    {group.statements.map((statement) => (
                      <li key={statement.statementId} className="statement-item">
                        <div>
                          <p className="item-title">{formatStatementPeriod(statement.periodYearMonth)} · Version {statement.artifactVersion}</p>
                          <p className="item-meta">Generated {formatDateTime(statement.generatedAtUtc)}</p>
                        </div>
                        <span className={statement.status === "FAILED" ? "status-pill status-pill--warn" : "status-pill status-pill--ok"}>
                          {statement.status}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </article>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}

function describeAccount(account: BankAccount): string {
  const segments = [
    account.accountType,
    account.accountNumberMasked,
    account.status
  ];

  if (typeof account.interestRate === "number" && account.accountType.toLowerCase().includes("saving")) {
    segments.push(`Interest ${account.interestRate.toFixed(2)}%`);
  }

  if (typeof account.checkingNumber === "number") {
    segments.push(`Checking #${account.checkingNumber}`);
  }

  return segments.join(" · ");
}
