import { FormEvent, useEffect, useMemo, useState } from "react";
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
  const primaryAccountId = accounts[0]?.accountId ?? "";

  const [paymentForm, setPaymentForm] = useState<TransferInput>({
    sourceAccountId: "",
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

  useEffect(() => {
    if (!hasAccounts) {
      return;
    }

    setPaymentForm((previous) => {
      const nextSource = previous.sourceAccountId && accounts.some((account) => account.accountId === previous.sourceAccountId)
        ? previous.sourceAccountId
        : primaryAccountId;

      const eligibleDestinations = accounts.filter((account) => account.accountId !== nextSource);
      const nextDestination = previous.destinationAccountId
        && eligibleDestinations.some((account) => account.accountId === previous.destinationAccountId)
        ? previous.destinationAccountId
        : (eligibleDestinations[0]?.accountId ?? "");

      if (nextSource === previous.sourceAccountId && nextDestination === previous.destinationAccountId) {
        return previous;
      }

      return {
        ...previous,
        sourceAccountId: nextSource,
        destinationAccountId: nextDestination
      };
    });
  }, [accounts, hasAccounts, primaryAccountId]);

  const destinationOptions = accounts.filter((account) => account.accountId !== paymentForm.sourceAccountId);
  const canTransfer = hasAccounts && destinationOptions.length > 0;
  const transactionsCurrency = accounts[0]?.currency ?? "USD";

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canTransfer) {
      setFeedback("At least two active accounts are required to transfer funds.");
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
        amount
      });
      setFeedback(`Payment completed. Reference ${receipt.reference}.`);
      setPaymentForm((previous) => ({ ...previous, amount: 0, note: "" }));
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
                value={paymentForm.sourceAccountId}
                onChange={(event) => setPaymentForm({ ...paymentForm, sourceAccountId: event.target.value })}
                disabled={!hasAccounts || accountsQuery.isPending || accountsQuery.isError}
              >
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountName} ({account.accountNumberMasked})
                  </option>
                ))}
              </select>
            </label>

            <label>
              To account
              <select
                value={paymentForm.destinationAccountId}
                onChange={(event) =>
                  setPaymentForm({ ...paymentForm, destinationAccountId: event.target.value })
                }
                disabled={!canTransfer || accountsQuery.isPending || accountsQuery.isError}
                required
              >
                {!canTransfer && <option value="">No destination accounts available</option>}
                {destinationOptions.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountName} ({account.accountNumberMasked})
                  </option>
                ))}
              </select>
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
                  value={paymentForm.note ?? ""}
                  onChange={(event) => setPaymentForm({ ...paymentForm, note: event.target.value })}
                  placeholder="Optional"
                />
              </label>
            </div>

            <div className="actions">
              <button type="submit" disabled={transferMutation.isPending || !canTransfer || accountsQuery.isPending || accountsQuery.isError}>
                {transferMutation.isPending ? "Submitting..." : "Confirm payment"}
              </button>
            </div>
          </form>
          {accountsQuery.isError ? (
            <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
          ) : !hasAccounts ? (
            <p className="hint-text">No accounts found for customer. Open an account from Accounts first.</p>
          ) : !canTransfer ? (
            <p className="hint-text">Open another account before creating transfers.</p>
          ) : null}
          <p className="hint-text">{feedback}</p>
        </article>

        <article className="surface-card">
          <h3>Recent transactions</h3>
          <p className="hint-text">Outgoing this period: {formatCurrency(totalOutgoing, transactionsCurrency)}</p>
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
