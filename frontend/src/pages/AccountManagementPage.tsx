import { useQuery } from "@tanstack/react-query";
import { fetchAccounts } from "../services/accounts";
import { formatCurrency } from "../utils/formatting";

export function AccountManagementPage() {
  const accountsQuery = useQuery({
    queryKey: ["accounts"],
    queryFn: fetchAccounts
  });

  const accounts = accountsQuery.data ?? [];

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Accounts</h2>
          <p className="page-subtitle">Track balances and stay on top of account health.</p>
        </div>
      </header>

      <section className="account-grid">
        {accounts.map((account) => (
          <article key={account.accountId} className="account-card">
            <div className="account-card-header">
              <p className="item-title">{account.accountName}</p>
              <span className={account.status === "Active" ? "status-pill status-pill--ok" : "status-pill"}>
                {account.status}
              </span>
            </div>
            <p className="item-meta">{account.accountType} · {account.accountNumberMasked}</p>
            <p className="balance-value">{formatCurrency(account.availableBalance, account.currency)}</p>
            <p className="balance-caption">
              Current balance {formatCurrency(account.currentBalance, account.currency)}
            </p>
          </article>
        ))}
      </section>
    </section>
  );
}
