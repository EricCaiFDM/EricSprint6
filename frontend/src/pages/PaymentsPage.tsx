import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAccounts, type BankAccount } from "../services/accounts";
import { fetchCustomersForAdmin } from "../services/customers";
import { fetchRecentNotifications } from "../services/notifications";
import {
  fetchTransactionHistory,
  submitDeposit,
  submitTransfer,
  submitWithdrawal,
  type PostingReceipt,
  type TransactionHistoryQuery,
  type TransactionHistoryResult,
  type TransactionItem,
  type TransactionType,
  type TransactionTypeFilter,
  type TransferReceipt
} from "../services/transactions";
import { getNormalizedTokenRole } from "../services/session";
import {
  filterCustomersByNameOrId,
  formatCustomerScopeOption,
  resolveCustomerIdFromScopeInput
} from "../utils/customerScope";
import { formatCurrency, formatDate } from "../utils/formatting";

type OperationReceipt = {
  kind: "deposit" | "withdrawal" | "transfer";
  receipt: PostingReceipt | TransferReceipt;
};

const emptyHistory: TransactionHistoryResult = {
  items: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  totalPages: 1
};

export function PaymentsPage() {
  const queryClient = useQueryClient();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";
  const initialFeedbackMessage = "Deposit, withdraw, transfer, and review all transactions in one place.";

  const [customerScopeInput, setCustomerScopeInput] = useState("");
  const [selectedCustomerScopeId, setSelectedCustomerScopeId] = useState("");

  const [depositAccountId, setDepositAccountId] = useState("");
  const [depositAmount, setDepositAmount] = useState("");

  const [withdrawAccountId, setWithdrawAccountId] = useState("");
  const [withdrawAmount, setWithdrawAmount] = useState("");

  const [transferSourceAccountId, setTransferSourceAccountId] = useState("");
  const [transferDestinationAccountId, setTransferDestinationAccountId] = useState("");
  const [transferAmount, setTransferAmount] = useState("");

  const [historyScopeType, setHistoryScopeType] = useState<"CUSTOMER" | "ACCOUNT">("CUSTOMER");
  const [historyScopeId, setHistoryScopeId] = useState("");
  const [historyType, setHistoryType] = useState<TransactionTypeFilter>("ALL");
  const [historyStartDate, setHistoryStartDate] = useState("");
  const [historyEndDate, setHistoryEndDate] = useState("");
  const [historyPage, setHistoryPage] = useState(1);
  const [historyPageSize, setHistoryPageSize] = useState(10);

  const [feedback, setFeedback] = useState(initialFeedbackMessage);
  const [notificationFeedback, setNotificationFeedback] = useState<string | null>(null);
  const [lastOperation, setLastOperation] = useState<OperationReceipt | null>(null);
  const [depositError, setDepositError] = useState<string | null>(null);
  const [withdrawError, setWithdrawError] = useState<string | null>(null);
  const [transferError, setTransferError] = useState<string | null>(null);

  const adminCustomersQuery = useQuery({
    queryKey: ["customers", "admin", "payments-scope-options"],
    queryFn: () => fetchCustomersForAdmin(1, 200),
    enabled: isAdmin
  });

  const adminCustomers = adminCustomersQuery.data ?? [];

  const inferredCustomerScopeId = useMemo(
    () => resolveCustomerIdFromScopeInput(customerScopeInput, adminCustomers),
    [customerScopeInput, adminCustomers]
  );

  const customerScopeId = selectedCustomerScopeId || inferredCustomerScopeId;

  const matchingScopeCustomers = useMemo(
    () => filterCustomersByNameOrId(adminCustomers, customerScopeInput),
    [adminCustomers, customerScopeInput]
  );

  const accountsQuery = useQuery({
    queryKey: ["accounts", "payments", isAdmin ? customerScopeId : "self"],
    queryFn: () => fetchAccounts(isAdmin ? customerScopeId || undefined : undefined),
    enabled: !isAdmin || Boolean(customerScopeId.trim())
  });

  const accounts = accountsQuery.data ?? [];
  const hasAccounts = accounts.length > 0;

  useEffect(() => {
    if (!hasAccounts) {
      setDepositAccountId("");
      setWithdrawAccountId("");
      setTransferSourceAccountId("");
      setTransferDestinationAccountId("");
      if (historyScopeType === "ACCOUNT" && historyScopeId) {
        setHistoryScopeId("");
      }
      return;
    }

    const primary = accounts[0].accountId;
    const secondary = accounts.find((item) => item.accountId !== primary)?.accountId ?? "";

    const hasAccount = (accountId: string) => accounts.some((item) => item.accountId === accountId);

    if (!depositAccountId || !hasAccount(depositAccountId)) {
      setDepositAccountId(primary);
    }

    if (!withdrawAccountId || !hasAccount(withdrawAccountId)) {
      setWithdrawAccountId(primary);
    }

    if (!transferSourceAccountId || !hasAccount(transferSourceAccountId)) {
      setTransferSourceAccountId(primary);
    }

    if (!transferDestinationAccountId || !hasAccount(transferDestinationAccountId)) {
      setTransferDestinationAccountId(secondary);
    }

    if (historyScopeType === "ACCOUNT" && historyScopeId && !hasAccount(historyScopeId)) {
      setHistoryScopeId("");
    }
  }, [
    accounts,
    hasAccounts,
    depositAccountId,
    withdrawAccountId,
    transferSourceAccountId,
    transferDestinationAccountId,
    historyScopeType,
    historyScopeId
  ]);

  const destinationOptions = useMemo(
    () => accounts.filter((item) => item.accountId !== transferSourceAccountId),
    [accounts, transferSourceAccountId]
  );

  const accountNameById = useMemo(
    () => new Map(accounts.map((account) => [account.accountId, account.accountName])),
    [accounts]
  );

  useEffect(() => {
    if (!destinationOptions.length) {
      setTransferDestinationAccountId("");
      return;
    }

    if (!destinationOptions.some((item) => item.accountId === transferDestinationAccountId)) {
      setTransferDestinationAccountId(destinationOptions[0].accountId);
    }
  }, [destinationOptions, transferDestinationAccountId]);

  const historyQueryParams: TransactionHistoryQuery = useMemo(
    () => ({
      scopeType: historyScopeType,
      customerId: isAdmin ? customerScopeId || undefined : undefined,
      scopeId: historyScopeType === "ACCOUNT" ? historyScopeId || undefined : undefined,
      transactionType: historyType,
      startDate: historyStartDate || undefined,
      endDate: historyEndDate || undefined,
      page: historyPage,
      pageSize: historyPageSize
    }),
    [
      historyScopeType,
      customerScopeId,
      historyScopeId,
      historyType,
      historyStartDate,
      historyEndDate,
      historyPage,
      historyPageSize,
      isAdmin
    ]
  );

  const historyQuery = useQuery({
    queryKey: ["transactions", "history", historyQueryParams],
    queryFn: () => fetchTransactionHistory(historyQueryParams),
    enabled: (!isAdmin || Boolean(customerScopeId.trim())) && (historyScopeType === "CUSTOMER" || Boolean(historyScopeId)),
    placeholderData: (previous) => previous ?? emptyHistory
  });

  const applyBalancePatch = (entries: Array<{ accountId: string; balanceAfter: number }>) => {
    const patches = entries.filter(
      (entry) => entry.accountId.trim().length > 0 && Number.isFinite(entry.balanceAfter)
    );

    if (patches.length === 0) {
      return;
    }

    const patchMap = new Map<string, number>();
    patches.forEach((entry) => {
      patchMap.set(entry.accountId, entry.balanceAfter);
    });

    queryClient.setQueriesData<BankAccount[]>({ queryKey: ["accounts"] }, (current) => {
      if (!Array.isArray(current)) {
        return current;
      }

      return current.map((account) => {
        const nextBalance = patchMap.get(account.accountId);
        if (nextBalance === undefined) {
          return account;
        }

        return {
          ...account,
          availableBalance: nextBalance,
          currentBalance: nextBalance
        };
      });
    });

    patches.forEach((entry) => {
      queryClient.setQueryData<BankAccount>(["accounts", "details", entry.accountId], (current) => {
        if (!current) {
          return current;
        }

        return {
          ...current,
          availableBalance: entry.balanceAfter,
          currentBalance: entry.balanceAfter
        };
      });
    });
  };

  const refreshAll = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["accounts"] }),
      queryClient.invalidateQueries({ queryKey: ["transactions"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] })
    ]);
  };

  const refreshNotificationFeed = async (fallbackTitle: string) => {
    try {
      const latest = await fetchRecentNotifications();
      queryClient.setQueryData(["notification-feed"], latest);
      const title = latest[0]?.title ?? fallbackTitle;
      setNotificationFeedback(`Notification sent: ${title}.`);
    } catch {
      setNotificationFeedback(`Notification sent: ${fallbackTitle}.`);
    }
  };

  const depositMutation = useMutation({
    mutationFn: submitDeposit,
    onSuccess: async (receipt, variables) => {
      setDepositError(null);
      applyBalancePatch([
        {
          accountId: variables.accountId,
          balanceAfter: receipt.balanceAfter
        }
      ]);
      setLastOperation({ kind: "deposit", receipt });
      setFeedback(`Deposit completed. Reference ${receipt.reference}.`);
      setDepositAmount("");
      await Promise.all([
        refreshAll(),
        refreshNotificationFeed("Deposit Posted")
      ]);
    },
    onError: (error) => setDepositError(`Deposit failed: ${(error as Error).message}`)
  });

  const withdrawMutation = useMutation({
    mutationFn: submitWithdrawal,
    onSuccess: async (receipt, variables) => {
      setWithdrawError(null);
      applyBalancePatch([
        {
          accountId: variables.accountId,
          balanceAfter: receipt.balanceAfter
        }
      ]);
      setLastOperation({ kind: "withdrawal", receipt });
      setFeedback(`Withdrawal completed. Reference ${receipt.reference}.`);
      setWithdrawAmount("");
      await refreshAll();
    },
    onError: (error) => setWithdrawError(`Withdrawal failed: ${(error as Error).message}`)
  });

  const transferMutation = useMutation({
    mutationFn: submitTransfer,
    onSuccess: async (receipt, variables) => {
      setTransferError(null);
      applyBalancePatch([
        {
          accountId: variables.sourceAccountId,
          balanceAfter: receipt.sourceBalanceAfter
        },
        {
          accountId: variables.destinationAccountId,
          balanceAfter: receipt.destinationBalanceAfter
        }
      ]);
      setLastOperation({ kind: "transfer", receipt });
      setFeedback(`Transfer completed. Reference ${receipt.reference}.`);
      setTransferAmount("");
      await Promise.all([
        refreshAll(),
        refreshNotificationFeed("Transfer Completed")
      ]);
    },
    onError: (error) => setTransferError(`Transfer failed: ${(error as Error).message}`)
  });

  const onDepositSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setDepositError(null);

    depositMutation.mutate({
      accountId: depositAccountId,
      amount: Number(depositAmount),
      customerId: isAdmin ? customerScopeId.trim() || undefined : undefined
    });
  };

  const onWithdrawSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setWithdrawError(null);

    withdrawMutation.mutate({
      accountId: withdrawAccountId,
      amount: Number(withdrawAmount),
      customerId: isAdmin ? customerScopeId.trim() || undefined : undefined
    });
  };

  const onTransferSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setTransferError(null);

    transferMutation.mutate({
      sourceAccountId: transferSourceAccountId,
      destinationAccountId: transferDestinationAccountId,
      amount: Number(transferAmount),
      customerId: isAdmin ? customerScopeId.trim() || undefined : undefined
    });
  };

  const onHistoryFilterSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setHistoryPage(1);
  };

  const currentHistory = historyQuery.data ?? emptyHistory;

  const totalDebits = useMemo(
    () => currentHistory.items
      .filter((item) => item.direction === "DEBIT")
      .reduce((total, item) => total + item.amount, 0),
    [currentHistory.items]
  );

  const totalCredits = useMemo(
    () => currentHistory.items
      .filter((item) => item.direction === "CREDIT")
      .reduce((total, item) => total + item.amount, 0),
    [currentHistory.items]
  );

  const finalTotalAccountBalance = useMemo(
    () => accounts.reduce((total, account) => total + account.currentBalance, 0),
    [accounts]
  );

  const baseCurrency = accounts[0]?.currency ?? "USD";

  const canDeposit = hasAccounts && Boolean(depositAccountId) && Number(depositAmount) > 0 && !depositMutation.isPending;
  const canWithdraw = hasAccounts && Boolean(withdrawAccountId) && Number(withdrawAmount) > 0 && !withdrawMutation.isPending;
  const canTransfer =
    hasAccounts &&
    Boolean(transferSourceAccountId) &&
    Boolean(transferDestinationAccountId) &&
    destinationOptions.length > 0 &&
    transferSourceAccountId !== transferDestinationAccountId &&
    Number(transferAmount) > 0 &&
    !transferMutation.isPending;
  const showCustomerFeedbackAlert = !isAdmin && feedback !== initialFeedbackMessage;

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Payments & transactions</h2>
          <p className="page-subtitle">Deposit, withdraw, transfer funds, and retrieve full transaction history.</p>
        </div>
      </header>

      {notificationFeedback ? (
        <div className="in-page-alert" role="alert">
          <span>{notificationFeedback}</span>
          <button type="button" className="in-page-alert-dismiss" onClick={() => setNotificationFeedback(null)}>
            Dismiss
          </button>
        </div>
      ) : null}

      {showCustomerFeedbackAlert ? (
        <div className="in-page-alert" role="status">
          <span>{feedback}</span>
          <button
            type="button"
            className="in-page-alert-dismiss"
            onClick={() => setFeedback(initialFeedbackMessage)}
          >
            Dismiss
          </button>
        </div>
      ) : null}

      {isAdmin ? (
        <article className="surface-card">
          <h3>Admin customer scope</h3>
          <form className="form" onSubmit={(event) => event.preventDefault()}>
            <label>
              Target customer name or ID
              <input
                value={customerScopeInput}
                onChange={(event) => {
                  setCustomerScopeInput(event.target.value);
                  setSelectedCustomerScopeId("");
                }}
                placeholder="Search by customer name or ID"
              />
            </label>

            <label>
              Matching customers
              <select
                value={customerScopeId}
                onChange={(event) => setSelectedCustomerScopeId(event.target.value)}
                disabled={adminCustomersQuery.isPending || matchingScopeCustomers.length === 0}
              >
                <option value="">Select customer</option>
                {matchingScopeCustomers.map((customer) => (
                  <option key={customer.customerId} value={customer.customerId}>
                    {formatCustomerScopeOption(customer)}
                  </option>
                ))}
              </select>
            </label>
          </form>
          <p className="hint-text">Enter customer name or ID before posting or querying transactions as admin.</p>
          {customerScopeInput.trim() && !customerScopeId ? (
            <p className="hint-text">Select a customer from suggestions or provide an exact customer ID.</p>
          ) : null}
        </article>
      ) : null}

      <section className="summary-grid">
        <article className="summary-card">
          <p className="summary-label">History credits</p>
          <p className="summary-value">{formatCurrency(totalCredits, baseCurrency)}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">History debits</p>
          <p className="summary-value">{formatCurrency(totalDebits, baseCurrency)}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Final total account balance</p>
          <p className="summary-value">{formatCurrency(finalTotalAccountBalance, baseCurrency)}</p>
        </article>
      </section>

      <section className="payments-ops-grid">
        <article className="surface-card">
          <h3>Deposit funds</h3>
          <form className="form" onSubmit={onDepositSubmit}>
            <label>
              Account
              <select
                value={depositAccountId}
                onChange={(event) => setDepositAccountId(event.target.value)}
                disabled={accountsQuery.isPending || accountsQuery.isError || !hasAccounts}
              >
                <option value="">Select account</option>
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Amount
              <input
                type="number"
                min={0.01}
                step={0.01}
                value={depositAmount}
                onChange={(event) => setDepositAmount(event.target.value)}
                required
              />
            </label>

            <div className="actions">
              <button type="submit" disabled={!canDeposit}>
                {depositMutation.isPending ? "Depositing..." : "Submit deposit"}
              </button>
            </div>
            {depositError ? <p className="inline-error" role="alert">{depositError}</p> : null}
          </form>
        </article>

        <article className="surface-card">
          <h3>Withdraw funds</h3>
          <form className="form" onSubmit={onWithdrawSubmit}>
            <label>
              Account
              <select
                value={withdrawAccountId}
                onChange={(event) => setWithdrawAccountId(event.target.value)}
                disabled={accountsQuery.isPending || accountsQuery.isError || !hasAccounts}
              >
                <option value="">Select account</option>
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Amount
              <input
                type="number"
                min={0.01}
                step={0.01}
                value={withdrawAmount}
                onChange={(event) => setWithdrawAmount(event.target.value)}
                required
              />
            </label>

            <div className="actions">
              <button type="submit" disabled={!canWithdraw}>
                {withdrawMutation.isPending ? "Withdrawing..." : "Submit withdrawal"}
              </button>
            </div>
            {withdrawError ? <p className="inline-error" role="alert">{withdrawError}</p> : null}
          </form>
        </article>

        <article className="surface-card payments-transfer-card">
          <h3>Transfer funds</h3>
          <form className="form" onSubmit={onTransferSubmit}>
            <label>
              Source account
              <select
                value={transferSourceAccountId}
                onChange={(event) => setTransferSourceAccountId(event.target.value)}
                disabled={accountsQuery.isPending || accountsQuery.isError || !hasAccounts}
              >
                <option value="">Select source account</option>
                {!hasAccounts && <option value="">No accounts available</option>}
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Destination account
              <select
                value={transferDestinationAccountId}
                onChange={(event) => setTransferDestinationAccountId(event.target.value)}
                disabled={accountsQuery.isPending || accountsQuery.isError || !destinationOptions.length}
              >
                <option value="">Select destination account</option>
                {!destinationOptions.length && <option value="">No destination account available</option>}
                {destinationOptions.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Amount
              <input
                type="number"
                min={0.01}
                step={0.01}
                value={transferAmount}
                onChange={(event) => setTransferAmount(event.target.value)}
                required
              />
            </label>

            <div className="actions">
              <button type="submit" disabled={!canTransfer}>
                {transferMutation.isPending ? "Transferring..." : "Submit transfer"}
              </button>
            </div>
            {transferError ? <p className="inline-error" role="alert">{transferError}</p> : null}
          </form>
        </article>
      </section>

      <article className="surface-card">
        <h3>Transaction history</h3>
        <form className="history-filter-grid" onSubmit={onHistoryFilterSubmit}>
          <label>
            Scope
            <select
              value={historyScopeType}
              onChange={(event) => {
                const nextScopeType = event.target.value as "CUSTOMER" | "ACCOUNT";
                setHistoryScopeType(nextScopeType);
                setHistoryPage(1);
                if (nextScopeType === "CUSTOMER") {
                  setHistoryScopeId("");
                }
              }}
            >
              <option value="CUSTOMER">Customer</option>
              <option value="ACCOUNT">Account</option>
            </select>
          </label>

          {historyScopeType === "ACCOUNT" && (
            <label>
              Account
              <select
                value={historyScopeId}
                onChange={(event) => {
                  setHistoryScopeId(event.target.value);
                  setHistoryPage(1);
                }}
                required
              >
                <option value="">Select account</option>
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label>
            Transaction type
            <select
              value={historyType}
              onChange={(event) => {
                setHistoryType(event.target.value as TransactionTypeFilter);
                setHistoryPage(1);
              }}
            >
              <option value="ALL">All</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAWAL">Withdrawal</option>
              <option value="TRANSFER_DEBIT">Transfer debit</option>
              <option value="TRANSFER_CREDIT">Transfer credit</option>
            </select>
          </label>

          <label>
            Start date
            <input
              type="date"
              value={historyStartDate}
              onChange={(event) => setHistoryStartDate(event.target.value)}
            />
          </label>

          <label>
            End date
            <input
              type="date"
              value={historyEndDate}
              onChange={(event) => setHistoryEndDate(event.target.value)}
            />
          </label>

          <label>
            Page size
            <select
              value={historyPageSize}
              onChange={(event) => {
                setHistoryPageSize(Number(event.target.value));
                setHistoryPage(1);
              }}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </label>

          <div className="actions">
            <button type="submit">Apply filters</button>
          </div>
        </form>

        {historyQuery.isLoading ? (
          <p className="hint-text">Loading transaction history...</p>
        ) : historyQuery.isError ? (
          <p className="hint-text">Unable to load history: {(historyQuery.error as Error).message}</p>
        ) : currentHistory.items.length === 0 ? (
          <p className="hint-text">No transactions found for selected filters.</p>
        ) : (
          <ul className="activity-list">
            {currentHistory.items.map((item) => (
              <li key={item.transactionId} className="activity-item">
                <div>
                  <p className="item-title">{formatHistoryTitle(item)}</p>
                  <p className="item-meta">{formatDate(item.bookedAt)} · Ref {item.transactionId}</p>
                  <p className="item-meta">
                    {formatHistoryAccountLabel(item, accountNameById, historyScopeType, historyScopeId)}
                  </p>
                </div>
                <p className={item.direction === "CREDIT" ? "amount-credit" : "amount-debit"}>
                  {item.direction === "CREDIT" ? "+" : "-"}
                  {formatCurrency(item.amount, item.currency)}
                </p>
              </li>
            ))}
          </ul>
        )}

        <div className="payments-pagination">
          <button
            type="button"
            className="button-secondary"
            onClick={() => setHistoryPage((current) => Math.max(1, current - 1))}
            disabled={historyPage <= 1 || historyQuery.isFetching}
          >
            Previous
          </button>
          <p className="hint-text">
            Page {currentHistory.page} of {currentHistory.totalPages} · {currentHistory.totalItems} total
          </p>
          <button
            type="button"
            className="button-secondary"
            onClick={() => setHistoryPage((current) => Math.min(currentHistory.totalPages, current + 1))}
            disabled={historyPage >= currentHistory.totalPages || historyQuery.isFetching}
          >
            Next
          </button>
        </div>
      </article>

      {isAdmin ? (
        <article className="surface-card">
          <h3>Operation status</h3>
          <p className="hint-text">{feedback}</p>
          {accountsQuery.isError && (
            <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
          )}
          {!hasAccounts && !accountsQuery.isPending && !accountsQuery.isError && (
            <p className="hint-text">No accounts found for customer. Create an account before posting transactions.</p>
          )}
          {lastOperation ? (
            <dl className="profile-grid payments-receipt-grid">
              <div>
                <dt>Operation</dt>
                <dd>{toReadableOperation(lastOperation.kind)}</dd>
              </div>
              <div>
                <dt>Reference</dt>
                <dd>{lastOperation.receipt.reference}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{lastOperation.receipt.status}</dd>
              </div>
              <div>
                <dt>Submitted</dt>
                <dd>{formatDate(lastOperation.receipt.submittedAt)}</dd>
              </div>
            </dl>
          ) : null}
        </article>
      ) : null}
    </section>
  );
}

function toReadableType(type: TransactionType): string {
  switch (type) {
    case "DEPOSIT":
      return "Deposit";
    case "WITHDRAWAL":
      return "Withdrawal";
    case "TRANSFER_DEBIT":
      return "Transfer debit";
    case "TRANSFER_CREDIT":
      return "Transfer credit";
    default:
      return type;
  }
}

function toReadableOperation(kind: OperationReceipt["kind"]): string {
  switch (kind) {
    case "deposit":
      return "Deposit";
    case "withdrawal":
      return "Withdrawal";
    case "transfer":
      return "Transfer";
    default:
      return kind;
  }
}

function formatHistoryTitle(item: TransactionItem): string {
  const typeLabel = toReadableType(item.transactionType);
  const description = item.description?.trim() ?? "";

  if (!description) {
    return typeLabel;
  }

  if (description.toLocaleLowerCase() === typeLabel.toLocaleLowerCase()) {
    return typeLabel;
  }

  return `${typeLabel} · ${description}`;
}

function formatAccountLabel(account: BankAccount): string {
  return `${account.accountName} (${account.accountNumberMasked}) · Balance ${formatCurrency(account.currentBalance, account.currency)}`;
}

function formatHistoryAccountLabel(
  item: TransactionItem,
  accountNameById: Map<string, string>,
  scopeType: "CUSTOMER" | "ACCOUNT",
  scopeId: string
): string {
  const resolvedAccountId = item.accountId?.trim() || (scopeType === "ACCOUNT" ? scopeId.trim() : "");

  if (!resolvedAccountId) {
    return "Account: unavailable";
  }

  const resolvedName = accountNameById.get(resolvedAccountId);
  if (!resolvedName) {
    return `Account ID: ${resolvedAccountId}`;
  }

  return `Account: ${resolvedName} (${resolvedAccountId})`;
}
