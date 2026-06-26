import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchAccounts } from "../services/accounts";
import {
  fetchRecentTransactions,
  submitTransfer,
  type TransferInput
} from "../services/transactions";
import { formatCurrency, formatDate } from "../utils/formatting";

export function PaymentsPage() {
  const accountsQuery = useQuery({
    queryKey: ["accounts"],
    queryFn: fetchAccounts
  });

  const transactionsQuery = useQuery({
    queryKey: ["transactions", "recent"],
    queryFn: fetchRecentTransactions
  });

  const accounts = accountsQuery.data ?? [];
  const hasAccounts = accounts.length > 0;
  const sourceAccountId = accounts[0]?.accountId ?? "";

  const [paymentForm, setPaymentForm] = useState<TransferInput>({
    sourceAccountId,
    recipientName: "",
    destinationAccountId: "",
    amount: 0,
    note: ""
  });
  const [feedback, setFeedback] = useState("Make a payment securely in just a few steps.");

  const transferMutation = useMutation({
    mutationFn: submitTransfer
  });

  const totalOutgoing = useMemo(
    () => (transactionsQuery.data ?? [])
      .filter((item) => item.direction === "DEBIT")
      .reduce((sum, item) => sum + item.amount, 0),
    [transactionsQuery.data]
  );

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!hasAccounts) {
      setFeedback("No accounts found for customer. Open an account before submitting a payment.");
      return;
    }

    const amount = Number(paymentForm.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setFeedback("Enter a valid payment amount.");
      return;
    }

    try {
      const receipt = await transferMutation.mutateAsync({
        ...paymentForm,
        amount,
        sourceAccountId: paymentForm.sourceAccountId || sourceAccountId
      });
      setFeedback(`Payment submitted. Reference ${receipt.reference}.`);
      setPaymentForm((previous) => ({ ...previous, amount: 0, destinationAccountId: "", note: "" }));
    } catch (error) {
      setFeedback(`Payment failed: ${(error as Error).message}`);
    }
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Payments</h2>
          <p className="page-subtitle">Send money, review recent activity, and monitor monthly spend.</p>
        </div>
      </header>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>New payment</h3>
          <form className="form" onSubmit={onSubmit}>
            <label>
              From account
              <select
                value={paymentForm.sourceAccountId || sourceAccountId}
                onChange={(event) => setPaymentForm({ ...paymentForm, sourceAccountId: event.target.value })}
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

            <label>
              Recipient name
              <input
                value={paymentForm.recipientName}
                onChange={(event) => setPaymentForm({ ...paymentForm, recipientName: event.target.value })}
                placeholder="e.g. Alex Morgan"
                required
              />
            </label>

            <label>
              Recipient account reference
              <input
                value={paymentForm.destinationAccountId}
                onChange={(event) =>
                  setPaymentForm({ ...paymentForm, destinationAccountId: event.target.value })
                }
                placeholder="Account or PayID"
                required
              />
            </label>

            <div className="inline-fields">
              <label>
                Amount
                <input
                  value={paymentForm.amount || ""}
                  onChange={(event) =>
                    setPaymentForm({ ...paymentForm, amount: Number(event.target.value) })
                  }
                  type="number"
                  min={0.01}
                  step={0.01}
                  required
                />
              </label>
              <label>
                Note
                <input
                  value={paymentForm.note}
                  onChange={(event) => setPaymentForm({ ...paymentForm, note: event.target.value })}
                  placeholder="Optional"
                />
              </label>
            </div>

            <div className="actions">
              <button type="submit" disabled={transferMutation.isPending || !hasAccounts || accountsQuery.isPending || accountsQuery.isError}>
                {transferMutation.isPending ? "Submitting..." : "Confirm payment"}
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
          <h3>Recent transactions</h3>
          <p className="hint-text">Outgoing this period: {formatCurrency(totalOutgoing, "AUD")}</p>
          <ul className="activity-list">
            {(transactionsQuery.data ?? []).slice(0, 6).map((item) => (
              <li key={item.transactionId} className="activity-item">
                <div>
                  <p className="item-title">{item.description}</p>
                  <p className="item-meta">{formatDate(item.bookedAt)} · {item.category}</p>
                </div>
                <p className={item.direction === "CREDIT" ? "amount-credit" : "amount-debit"}>
                  {item.direction === "CREDIT" ? "+" : "-"}
                  {formatCurrency(item.amount, item.currency)}
                </p>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </section>
  );
}
