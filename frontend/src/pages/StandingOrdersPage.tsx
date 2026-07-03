import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAccounts, type BankAccount } from "../services/accounts";
import {
  cancelStandingOrder,
  createStandingOrder,
  fetchStandingOrders,
  pauseStandingOrder,
  resumeStandingOrder,
  type CreateStandingOrderInput,
  type StandingOrder,
  type StandingOrderCadence
} from "../services/standingOrders";
import { formatCurrency, formatDateTime } from "../utils/formatting";

type StandingOrderFormState = {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: string;
  cadence: StandingOrderCadence;
  effectiveFromDate: string;
  effectiveFromTime: string;
  effectiveToDate: string;
  retryPolicyCode: string;
};

function todayDateInput(): string {
  return new Date().toISOString().slice(0, 10);
}

function toUtcDateTime(dateInputValue: string, timeInputValue: string): string | null {
  if (!dateInputValue || !timeInputValue) {
    return null;
  }

  const localDateTime = new Date(`${dateInputValue}T${timeInputValue}:00`);
  if (Number.isNaN(localDateTime.getTime())) {
    return null;
  }

  return localDateTime.toISOString();
}

function cadenceMonthlyFactor(cadence: StandingOrderCadence): number {
  if (cadence === "DAILY") {
    return 30;
  }
  if (cadence === "WEEKLY") {
    return 52 / 12;
  }
  return 1;
}

function cadenceLabel(cadence: StandingOrderCadence): string {
  if (cadence === "DAILY") {
    return "Daily";
  }
  if (cadence === "WEEKLY") {
    return "Weekly";
  }
  return "Monthly";
}

function resolveStatusLabel(order: StandingOrder): string {
  if (order.lifecycleState === "ACTIVE") {
    return "Active";
  }
  if (order.lifecycleState === "PAUSED") {
    return "Paused";
  }
  if (order.lifecycleState === "CANCELLED") {
    return "Cancelled";
  }
  return "Completed";
}

export function StandingOrdersPage() {
  const queryClient = useQueryClient();

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
  const canCreateStandingOrder = accounts.length > 1;

  const [form, setForm] = useState<StandingOrderFormState>({
    sourceAccountId: "",
    destinationAccountId: "",
    amount: "",
    cadence: "MONTHLY",
    effectiveFromDate: todayDateInput(),
    effectiveFromTime: "09:00",
    effectiveToDate: "",
    retryPolicyCode: "STANDARD"
  });
  const [feedback, setFeedback] = useState("Create recurring transfers between eligible accounts.");
  const [pendingCancellationOrder, setPendingCancellationOrder] = useState<StandingOrder | null>(null);

  useEffect(() => {
    if (accounts.length === 0) {
      return;
    }

    setForm((previous) => {
      const sourceAccountId = previous.sourceAccountId || accounts[0]?.accountId || "";
      let destinationAccountId = previous.destinationAccountId;

      if (!destinationAccountId || destinationAccountId === sourceAccountId) {
        destinationAccountId = accounts.find((account) => account.accountId !== sourceAccountId)?.accountId ?? "";
      }

      return {
        ...previous,
        sourceAccountId,
        destinationAccountId
      };
    });
  }, [accounts]);

  const createMutation = useMutation({
    mutationFn: createStandingOrder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["standing-orders"] });
      setFeedback("Standing order created successfully.");
      setForm((previous) => ({
        ...previous,
        amount: ""
      }));
    }
  });

  const lifecycleMutation = useMutation({
    mutationFn: async ({
      action,
      standingOrderId
    }: {
      action: "pause" | "resume" | "cancel";
      standingOrderId: string;
    }) => {
      if (action === "pause") {
        return pauseStandingOrder(standingOrderId);
      }
      if (action === "resume") {
        return resumeStandingOrder(standingOrderId);
      }
      return cancelStandingOrder(standingOrderId);
    },
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({ queryKey: ["standing-orders"] });
      if (variables.action === "cancel") {
        setPendingCancellationOrder(null);
      }
      setFeedback(`Standing order ${variables.action} action completed.`);
    },
    onError: (error, variables) => {
      setFeedback(`Unable to ${variables.action} standing order: ${(error as Error).message}`);
    }
  });

  const monthlyCommitted = useMemo(
    () => (ordersQuery.data ?? [])
      .filter((order) => order.lifecycleState === "ACTIVE")
      .reduce((sum, order) => sum + (order.amount * cadenceMonthlyFactor(order.cadence)), 0),
    [ordersQuery.data]
  );

  const configuredOrders = useMemo(
    () => (ordersQuery.data ?? []).filter((order) => order.lifecycleState !== "CANCELLED"),
    [ordersQuery.data]
  );

  const accountNameById = useMemo(() => new Map(accounts.map((account) => [account.accountId, account.accountName])), [accounts]);

  const pendingCancellationOrderLabel = useMemo(() => {
    if (!pendingCancellationOrder) {
      return "";
    }

    const sourceName = accountNameById.get(pendingCancellationOrder.sourceAccountId) ?? pendingCancellationOrder.sourceAccountId;
    const destinationName = accountNameById.get(pendingCancellationOrder.destinationAccountId) ?? pendingCancellationOrder.destinationAccountId;
    return `${sourceName} to ${destinationName}`;
  }, [accountNameById, pendingCancellationOrder]);

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canCreateStandingOrder) {
      setFeedback("At least two active accounts are required to create a standing order.");
      return;
    }

    if (!form.sourceAccountId || !form.destinationAccountId) {
      setFeedback("Select both source and destination accounts.");
      return;
    }

    if (form.sourceAccountId === form.destinationAccountId) {
      setFeedback("Source and destination accounts must be different.");
      return;
    }

    const amount = Number(form.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setFeedback("Enter a valid transfer amount.");
      return;
    }

    if (!form.effectiveFromDate) {
      setFeedback("Effective from date is required.");
      return;
    }

    if (!form.effectiveFromTime) {
      setFeedback("Execution time is required.");
      return;
    }

    const effectiveFromUtc = toUtcDateTime(form.effectiveFromDate, form.effectiveFromTime);
    if (!effectiveFromUtc) {
      setFeedback("Effective from date and time are invalid.");
      return;
    }

    const effectiveToUtc = form.effectiveToDate
      ? toUtcDateTime(form.effectiveToDate, form.effectiveFromTime)
      : null;

    if (form.effectiveToDate && !effectiveToUtc) {
      setFeedback("Effective to date and time are invalid.");
      return;
    }

    const payload: CreateStandingOrderInput = {
      sourceAccountId: form.sourceAccountId,
      destinationAccountId: form.destinationAccountId,
      amount,
      cadence: form.cadence,
      effectiveFromUtc,
      effectiveToUtc,
      retryPolicyCode: form.retryPolicyCode
    };

    try {
      await createMutation.mutateAsync(payload);
    } catch (error) {
      setFeedback(`Unable to create standing order: ${(error as Error).message}`);
    }
  };

  const onConfirmCancellation = () => {
    if (!pendingCancellationOrder) {
      return;
    }

    lifecycleMutation.mutate({
      action: "cancel",
      standingOrderId: pendingCancellationOrder.standingOrderId
    });
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Standing orders</h2>
          <p className="page-subtitle">Automate recurring transfers with lifecycle controls and execution visibility.</p>
        </div>
      </header>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Create standing order</h3>
          <form className="form" onSubmit={onSubmit}>
            <label>
              From account
              <select
                value={form.sourceAccountId}
                onChange={(event) => setForm({ ...form, sourceAccountId: event.target.value })}
                disabled={!hasAccounts || accountsQuery.isPending || accountsQuery.isError}
              >
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatStandingOrderAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              To account
              <select
                value={form.destinationAccountId}
                onChange={(event) => setForm({ ...form, destinationAccountId: event.target.value })}
                disabled={!hasAccounts || accountsQuery.isPending || accountsQuery.isError}
              >
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatStandingOrderAccountLabel(account)}
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
                  value={form.amount}
                  onChange={(event) => setForm({ ...form, amount: event.target.value })}
                  required
                />
              </label>

              <label>
                Cadence
                <select
                  value={form.cadence}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      cadence: event.target.value as StandingOrderCadence
                    })
                  }
                >
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                </select>
              </label>
            </div>

            <label>
              Effective from
              <input
                type="date"
                value={form.effectiveFromDate}
                onChange={(event) => setForm({ ...form, effectiveFromDate: event.target.value })}
                required
              />
            </label>

            <label>
              Occurs at (local time)
              <input
                type="time"
                value={form.effectiveFromTime}
                onChange={(event) => setForm({ ...form, effectiveFromTime: event.target.value })}
                required
              />
            </label>

            <label>
              Effective to (optional)
              <input
                type="date"
                value={form.effectiveToDate}
                onChange={(event) => setForm({ ...form, effectiveToDate: event.target.value })}
              />
            </label>

            <label>
              Retry policy
              <select
                value={form.retryPolicyCode}
                onChange={(event) => setForm({ ...form, retryPolicyCode: event.target.value })}
              >
                <option value="STANDARD">Standard</option>
                <option value="NO_RETRY">No retry</option>
              </select>
            </label>

            <div className="actions">
              <button type="submit" disabled={createMutation.isPending || !canCreateStandingOrder || accountsQuery.isPending || accountsQuery.isError}>
                {createMutation.isPending ? "Saving..." : "Create standing order"}
              </button>
            </div>
          </form>
          {accountsQuery.isError ? (
            <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
          ) : !canCreateStandingOrder ? (
            <p className="hint-text">At least two active accounts are required to create a standing order.</p>
          ) : null}
          <p className="hint-text">{feedback}</p>
        </article>

        <article className="surface-card">
          <h3>Configured standing orders</h3>
          <p className="hint-text">Estimated monthly committed amount: {formatCurrency(monthlyCommitted, "USD")}</p>
          <ul className="stack-list">
            {configuredOrders.map((order) => {
              const sourceName = accountNameById.get(order.sourceAccountId) ?? order.sourceAccountId;
              const destinationName = accountNameById.get(order.destinationAccountId) ?? order.destinationAccountId;
              const canPause = order.lifecycleState === "ACTIVE";
              const canResume = order.lifecycleState === "PAUSED";
              const canCancel = order.lifecycleState === "ACTIVE" || order.lifecycleState === "PAUSED";

              return (
              <li className="stack-list-item" key={order.standingOrderId}>
                <div>
                  <p className="item-title">{sourceName} to {destinationName}</p>
                  <p className="item-meta">
                    {cadenceLabel(order.cadence)} · Next run {order.nextExecutionAtUtc ? formatDateTime(order.nextExecutionAtUtc) : "none"}
                  </p>
                </div>
                <div className="stack-list-meta">
                  <p className="item-emphasis">{formatCurrency(order.amount, "USD")}</p>
                  <span className={order.lifecycleState === "ACTIVE" ? "status-pill status-pill--ok" : "status-pill"}>
                    {resolveStatusLabel(order)}
                  </span>
                  <div className="actions">
                    {canPause ? (
                      <button
                        type="button"
                        disabled={lifecycleMutation.isPending}
                        onClick={() => lifecycleMutation.mutate({ action: "pause", standingOrderId: order.standingOrderId })}
                      >
                        Pause
                      </button>
                    ) : null}
                    {canResume ? (
                      <button
                        type="button"
                        disabled={lifecycleMutation.isPending}
                        onClick={() => lifecycleMutation.mutate({ action: "resume", standingOrderId: order.standingOrderId })}
                      >
                        Resume
                      </button>
                    ) : null}
                    {canCancel ? (
                      <button
                        type="button"
                        disabled={lifecycleMutation.isPending}
                        onClick={() => setPendingCancellationOrder(order)}
                      >
                        Cancel
                      </button>
                    ) : null}
                  </div>
                </div>
              </li>
              );
            })}
          </ul>
          {ordersQuery.isError ? <p className="hint-text">Unable to load standing orders.</p> : null}
        </article>
      </section>

      {pendingCancellationOrder ? (
        <div className="modal-backdrop" role="presentation">
          <section
            className="confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cancel-standing-order-title"
            aria-describedby="cancel-standing-order-description"
          >
            <h3 id="cancel-standing-order-title">Cancel standing order?</h3>
            <p id="cancel-standing-order-description">
              This will stop all future executions for {pendingCancellationOrderLabel}.
            </p>
            <p>You can create a new standing order later if needed.</p>
            <div className="actions">
              <button
                type="button"
                className="button-secondary"
                onClick={() => setPendingCancellationOrder(null)}
                disabled={lifecycleMutation.isPending}
              >
                Keep order
              </button>
              <button type="button" onClick={onConfirmCancellation} disabled={lifecycleMutation.isPending}>
                {lifecycleMutation.isPending ? "Cancelling..." : "Yes, cancel order"}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}

function formatStandingOrderAccountLabel(account: BankAccount): string {
  return `${account.accountName} (${account.accountNumberMasked}) · Balance ${formatCurrency(account.currentBalance, account.currency)}`;
}
