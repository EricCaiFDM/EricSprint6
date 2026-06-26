import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchAccounts } from "../services/accounts";
import {
  createStandingOrder,
  fetchStandingOrders,
  type CreateStandingOrderInput
} from "../services/standingOrders";
import { formatCurrency, formatDate } from "../utils/formatting";

export function StandingOrdersPage() {
  const accountsQuery = useQuery({
    queryKey: ["accounts"],
    queryFn: () => fetchAccounts()
  });
  const ordersQuery = useQuery({
    queryKey: ["standing-orders"],
    queryFn: fetchStandingOrders
  });

  const accounts = accountsQuery.data ?? [];
  const hasAccounts = accounts.length > 0;

  const [form, setForm] = useState<CreateStandingOrderInput>({
    payeeName: "",
    sourceAccountId: accounts[0]?.accountId ?? "",
    amount: 0,
    frequency: "Monthly",
    nextRunAt: new Date().toISOString().slice(0, 10)
  });
  const [feedback, setFeedback] = useState("Set up recurring payments for bills and savings.");

  const createMutation = useMutation({
    mutationFn: createStandingOrder
  });

  const monthlyCommitted = useMemo(
    () => (ordersQuery.data ?? [])
      .filter((order) => order.status === "Active")
      .reduce((sum, order) => sum + order.amount, 0),
    [ordersQuery.data]
  );

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!hasAccounts) {
      setFeedback("No accounts found for customer. Open an account before creating a scheduled payment.");
      return;
    }

    const amount = Number(form.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setFeedback("Enter a valid recurring amount.");
      return;
    }

    try {
      await createMutation.mutateAsync({
        ...form,
        amount,
        sourceAccountId: form.sourceAccountId || accounts[0]?.accountId || "",
        nextRunAt: new Date(form.nextRunAt).toISOString()
      });
      setFeedback("Recurring payment saved.");
      setForm((previous) => ({ ...previous, payeeName: "", amount: 0 }));
    } catch (error) {
      setFeedback(`Unable to save recurring payment: ${(error as Error).message}`);
    }
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Scheduled payments</h2>
          <p className="page-subtitle">Automate recurring bills and savings transfers with full visibility.</p>
        </div>
      </header>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Create scheduled payment</h3>
          <form className="form" onSubmit={onSubmit}>
            <label>
              Payee
              <input
                value={form.payeeName}
                onChange={(event) => setForm({ ...form, payeeName: event.target.value })}
                placeholder="e.g. Citywide Rent"
                required
              />
            </label>

            <label>
              From account
              <select
                value={form.sourceAccountId || accounts[0]?.accountId || ""}
                onChange={(event) => setForm({ ...form, sourceAccountId: event.target.value })}
                disabled={!hasAccounts || accountsQuery.isPending || accountsQuery.isError}
              >
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountName}
                  </option>
                ))}
              </select>
            </label>

            <div className="inline-fields">
              <label>
                Amount
                <input
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={form.amount || ""}
                  onChange={(event) => setForm({ ...form, amount: Number(event.target.value) })}
                  required
                />
              </label>

              <label>
                Frequency
                <select
                  value={form.frequency}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      frequency: event.target.value as CreateStandingOrderInput["frequency"]
                    })
                  }
                >
                  <option value="Weekly">Weekly</option>
                  <option value="Fortnightly">Fortnightly</option>
                  <option value="Monthly">Monthly</option>
                </select>
              </label>
            </div>

            <label>
              First payment date
              <input
                type="date"
                value={form.nextRunAt}
                onChange={(event) => setForm({ ...form, nextRunAt: event.target.value })}
                required
              />
            </label>

            <div className="actions">
              <button type="submit" disabled={createMutation.isPending || !hasAccounts || accountsQuery.isPending || accountsQuery.isError}>
                {createMutation.isPending ? "Saving..." : "Save schedule"}
              </button>
            </div>
          </form>
          {accountsQuery.isError ? (
            <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
          ) : !hasAccounts ? (
            <p className="hint-text">No accounts found for customer. Open an account from Accounts first.</p>
          ) : null}
          <p className="hint-text">{feedback}</p>
        </article>

        <article className="surface-card">
          <h3>Active schedules</h3>
          <p className="hint-text">Monthly committed amount: {formatCurrency(monthlyCommitted, "AUD")}</p>
          <ul className="stack-list">
            {(ordersQuery.data ?? []).map((order) => (
              <li className="stack-list-item" key={order.standingOrderId}>
                <div>
                  <p className="item-title">{order.payeeName}</p>
                  <p className="item-meta">{order.frequency} · Next run {formatDate(order.nextRunAt)}</p>
                </div>
                <div className="stack-list-meta">
                  <p className="item-emphasis">{formatCurrency(order.amount, order.currency)}</p>
                  <span className={order.status === "Active" ? "status-pill status-pill--ok" : "status-pill"}>
                    {order.status}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </section>
  );
}
