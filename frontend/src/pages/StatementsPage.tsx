import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAccounts, type BankAccount } from "../services/accounts";
import {
  fetchStatement,
  fetchStatementPdf,
  fetchStatementTransactions,
  fetchStatements,
  generateStatement,
  type StatementDetail,
  type StatementGenerationMode,
  type StatementListResult
} from "../services/statements";
import type { TransactionItem } from "../services/transactions";
import { formatCurrency, formatDateTime } from "../utils/formatting";

const emptyStatements: StatementListResult = {
  items: [],
  page: 1,
  pageSize: 20,
  totalItems: 0,
  totalPages: 1
};

export function StatementsPage() {
  const queryClient = useQueryClient();
  const [accountId, setAccountId] = useState("");
  const [periodYearMonth, setPeriodYearMonth] = useState("");
  const [generationMode, setGenerationMode] = useState<StatementGenerationMode>("STANDARD");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selectedStatementId, setSelectedStatementId] = useState("");
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState("Generate and retrieve monthly statements for your accounts.");

  const accountsQuery = useQuery({
    queryKey: ["accounts", "statements", "scope"],
    queryFn: () => fetchAccounts()
  });

  const accounts = accountsQuery.data ?? [];
  const hasAccounts = accounts.length > 0;

  useEffect(() => {
    if (!hasAccounts) {
      setAccountId("");
      return;
    }

    if (!accountId || !accounts.some((item) => item.accountId === accountId)) {
      setAccountId(accounts[0].accountId);
      setPage(1);
      setSelectedStatementId("");
    }
  }, [accounts, hasAccounts, accountId]);

  const statementsQuery = useQuery({
    queryKey: ["statements", accountId, periodYearMonth, page, pageSize],
    queryFn: () =>
      fetchStatements({
        accountId,
        periodYearMonth: periodYearMonth || undefined,
        page,
        pageSize
      }),
    enabled: Boolean(accountId)
  });

  const statementDetailQuery = useQuery({
    queryKey: ["statements", "detail", selectedStatementId],
    queryFn: () => fetchStatement(selectedStatementId),
    enabled: Boolean(selectedStatementId)
  });

  const statementTransactionsQuery = useQuery({
    queryKey: ["statements", "detail", "transactions", selectedStatementId],
    queryFn: () =>
      fetchStatementTransactions({
        accountId: statementDetailQuery.data?.accountId ?? "",
        periodYearMonth: statementDetailQuery.data?.periodYearMonth ?? ""
      }),
    enabled: Boolean(statementDetailQuery.data?.statementId)
  });

  const generateMutation = useMutation({
    mutationFn: generateStatement,
    onSuccess: async (result) => {
      setFeedback(`Statement generation ${result.generationStatus.toLowerCase()} for request ${result.statementId}.`);
      setSelectedStatementId(result.statementId);
      await queryClient.invalidateQueries({ queryKey: ["statements"] });
    },
    onError: (error) => {
      setFeedback(`Statement generation failed: ${(error as Error).message}`);
    }
  });

  const downloadPdfMutation = useMutation({
    mutationFn: async (statement: StatementDetail) => {
      const pdf = await fetchStatementPdf(statement);
      triggerPdfDownload(pdf.blob, pdf.fileName);
    },
    onMutate: () => {
      setDownloadError(null);
      setFeedback("Preparing statement PDF download...");
    },
    onSuccess: () => {
      setFeedback("Statement PDF download started.");
    },
    onError: (error) => {
      const message = (error as Error).message;
      const fullMessage = `Unable to download statement PDF: ${message}`;
      setDownloadError(fullMessage);
      setFeedback(fullMessage);
    }
  });

  const statements = statementsQuery.data ?? emptyStatements;

  const selectedAccount = useMemo(
    () => accounts.find((item) => item.accountId === accountId) ?? null,
    [accounts, accountId]
  );

  const canGenerate =
    Boolean(accountId) &&
    Boolean(periodYearMonth) &&
    Boolean(selectedAccount) &&
    !accountsQuery.isPending &&
    !generateMutation.isPending;

  const onGenerateSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const refreshed = await accountsQuery.refetch();
    const latestAccounts = refreshed.data ?? [];
    const selectedStillExists = latestAccounts.some((account) => account.accountId === accountId);

    if (!selectedStillExists) {
      const fallbackAccountId = latestAccounts[0]?.accountId ?? "";
      setAccountId(fallbackAccountId);
      setFeedback("The selected account is no longer available. Please reselect an account and try again.");
      return;
    }

    generateMutation.mutate({
      accountId,
      periodYearMonth,
      generationMode
    });
  };

  const onApplyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(1);
    setSelectedStatementId("");
  };

  const onViewDetails = (statementId: string) => {
    setSelectedStatementId(statementId);
    setDownloadError(null);
    setFeedback("Retrieving selected statement details and transactions...");
  };

  const onDownloadPdf = () => {
    if (!statementDetailQuery.data) {
      return;
    }

    downloadPdfMutation.mutate(statementDetailQuery.data);
  };

  const hasPreviousPage = statements.page > 1;
  const hasNextPage = statements.page < statements.totalPages;

  const summaryCurrency = selectedAccount?.currency ?? "USD";
  const generatedCount = statements.items.filter((item) => item.status !== "FAILED").length;
  const failedCount = statements.items.filter((item) => item.status === "FAILED").length;

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Statements</h2>
          <p className="page-subtitle">Generate monthly statements and retrieve authorized statement versions.</p>
        </div>
      </header>

      <p className="output">{feedback}</p>

      <section className="summary-grid">
        <article className="summary-card">
          <p className="summary-label">Statements in view</p>
          <p className="summary-value">{statements.items.length}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Generated or corrected</p>
          <p className="summary-value">{generatedCount}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Failed generations</p>
          <p className="summary-value">{failedCount}</p>
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Filter statements</h3>
          <form className="form" onSubmit={onApplyFilters}>
            <label>
              Account
              <select
                value={accountId}
                onChange={(event) => {
                  setAccountId(event.target.value);
                  setPage(1);
                  setSelectedStatementId("");
                }}
                disabled={accountsQuery.isPending || accountsQuery.isError || !hasAccounts}
              >
                <option value="">Select account</option>
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {formatAccountLabel(account)}
                  </option>
                ))}
              </select>
            </label>

            <div className="inline-fields">
              <label>
                Period (optional)
                <input
                  type="month"
                  value={periodYearMonth}
                  onChange={(event) => setPeriodYearMonth(event.target.value)}
                />
              </label>

              <label>
                Page size
                <select
                  value={pageSize}
                  onChange={(event) => {
                    setPageSize(Number(event.target.value));
                    setPage(1);
                  }}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </label>
            </div>

            <div className="actions">
              <button type="submit">Apply filters</button>
              <button
                type="button"
                className="button-secondary"
                onClick={() => {
                  setPeriodYearMonth("");
                  setPage(1);
                  setSelectedStatementId("");
                }}
              >
                Clear period
              </button>
            </div>

            {accountsQuery.isError ? (
              <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
            ) : null}
          </form>
        </article>

        <article className="surface-card">
          <h3>Generate statement</h3>
          <form className="form" onSubmit={onGenerateSubmit}>
            <label>
              Generation period
              <input
                type="month"
                value={periodYearMonth}
                onChange={(event) => setPeriodYearMonth(event.target.value)}
                required
              />
            </label>

            <label>
              Generation mode
              <select
                value={generationMode}
                onChange={(event) => setGenerationMode(event.target.value as StatementGenerationMode)}
              >
                <option value="STANDARD">STANDARD</option>
                <option value="CORRECTION">CORRECTION</option>
              </select>
            </label>

            <div className="actions">
              <button type="submit" disabled={!canGenerate}>
                {generateMutation.isPending ? "Submitting..." : "Generate statement"}
              </button>
            </div>

            {!selectedAccount ? (
              <p className="hint-text">Select an account before generating statements.</p>
            ) : (
              <p className="hint-text">
                Statements are generated for {selectedAccount.accountName} in {summaryCurrency}.
              </p>
            )}
          </form>
        </article>
      </section>

      <article className="surface-card">
        <h3>Statement results</h3>
        <ul className="statement-list">
          {statementsQuery.isPending ? <li className="statement-item">Loading statements...</li> : null}
          {statementsQuery.isError ? (
            <li className="statement-item">Unable to load statements: {(statementsQuery.error as Error).message}</li>
          ) : null}
          {!statementsQuery.isPending && !statementsQuery.isError && statements.items.length === 0 ? (
            <li className="statement-item">No statements found for the selected filters.</li>
          ) : null}
          {statements.items.map((statement) => (
            <li key={statement.statementId} className="statement-item">
              <div>
                <p className="item-title">
                  {statement.periodYearMonth} · Version {statement.artifactVersion}
                </p>
                <p className="item-meta">
                  Account {statement.accountId} · Generated {formatDateTime(statement.generatedAtUtc)}
                </p>
              </div>
              <div className="statement-item-right">
                <span className={statement.status === "FAILED" ? "status-pill status-pill--warn" : "status-pill status-pill--ok"}>
                  {statement.status}
                </span>
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => onViewDetails(statement.statementId)}
                >
                  View details
                </button>
              </div>
            </li>
          ))}
        </ul>

        <div className="payments-pagination">
          <button type="button" className="button-secondary" onClick={() => setPage((value) => value - 1)} disabled={!hasPreviousPage}>
            Previous
          </button>
          <p className="hint-text">
            Page {statements.page} of {Math.max(1, statements.totalPages)} · {statements.totalItems} total
          </p>
          <button type="button" className="button-secondary" onClick={() => setPage((value) => value + 1)} disabled={!hasNextPage}>
            Next
          </button>
        </div>
      </article>

      <article className="surface-card">
        <h3>Selected statement details</h3>
        {!selectedStatementId ? <p className="hint-text">Select a statement from the list to retrieve details.</p> : null}
        {selectedStatementId && statementDetailQuery.isPending ? (
          <p className="hint-text">Retrieving selected statement...</p>
        ) : null}
        {statementDetailQuery.isError ? (
          <p className="hint-text">Unable to retrieve statement: {(statementDetailQuery.error as Error).message}</p>
        ) : null}
        {statementDetailQuery.data ? (
          <>
            <dl className="profile-grid">
              <div>
                <dt>Statement ID</dt>
                <dd>{statementDetailQuery.data.statementId}</dd>
              </div>
              <div>
                <dt>Period</dt>
                <dd>{statementDetailQuery.data.periodYearMonth}</dd>
              </div>
              <div>
                <dt>Version</dt>
                <dd>{statementDetailQuery.data.artifactVersion}</dd>
              </div>
              <div>
                <dt>Opening balance</dt>
                <dd>{formatCurrency(statementDetailQuery.data.openingBalance, statementDetailQuery.data.currencyCode)}</dd>
              </div>
              <div>
                <dt>Closing balance</dt>
                <dd>{formatCurrency(statementDetailQuery.data.closingBalance, statementDetailQuery.data.currencyCode)}</dd>
              </div>
              <div>
                <dt>Generated at</dt>
                <dd>{formatDateTime(statementDetailQuery.data.generatedAtUtc)}</dd>
              </div>
            </dl>

            <div className="actions">
              <button
                type="button"
                className="button-secondary"
                onClick={onDownloadPdf}
                disabled={downloadPdfMutation.isPending}
              >
                {downloadPdfMutation.isPending ? "Preparing PDF..." : "Download PDF"}
              </button>
            </div>

            {downloadError ? <p className="inline-error" role="alert">{downloadError}</p> : null}

            <h4>Statement transactions</h4>
            {statementTransactionsQuery.isPending ? (
              <p className="hint-text">Loading statement transactions...</p>
            ) : null}
            {statementTransactionsQuery.isError ? (
              <p className="hint-text">Unable to load statement transactions: {(statementTransactionsQuery.error as Error).message}</p>
            ) : null}
            {!statementTransactionsQuery.isPending && !statementTransactionsQuery.isError && (statementTransactionsQuery.data?.length ?? 0) === 0 ? (
              <p className="hint-text">No transactions were posted in this statement period.</p>
            ) : null}
            {statementTransactionsQuery.data && statementTransactionsQuery.data.length > 0 ? (
              <ul className="activity-list">
                {statementTransactionsQuery.data.map((item) => (
                  <li key={item.transactionId} className="activity-item">
                    <div>
                      <p className="item-title">{toReadableStatementTransactionType(item)} · {item.description}</p>
                      <p className="item-meta">{formatDateTime(item.bookedAt)} · Ref {item.transactionId}</p>
                    </div>
                    <p className={item.direction === "CREDIT" ? "amount-credit" : "amount-debit"}>
                      {item.direction === "CREDIT" ? "+" : "-"}
                      {formatCurrency(item.amount, item.currency)}
                    </p>
                  </li>
                ))}
              </ul>
            ) : null}
          </>
        ) : null}
      </article>
    </section>
  );
}

function formatAccountLabel(account: BankAccount): string {
  return `${account.accountName} (${account.accountNumberMasked})`;
}

function toReadableStatementTransactionType(item: TransactionItem): string {
  switch (item.transactionType) {
    case "DEPOSIT":
      return "Deposit";
    case "WITHDRAWAL":
      return "Withdrawal";
    case "TRANSFER_DEBIT":
      return "Transfer / standing order debit";
    case "TRANSFER_CREDIT":
      return "Transfer / standing order credit";
    default:
      return "Transaction";
  }
}

function triggerPdfDownload(blob: Blob, fileName: string): void {
  if (!window.URL || typeof window.URL.createObjectURL !== "function") {
    throw new Error("PDF downloads are not supported in this browser.");
  }

  const objectUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");

  try {
    link.href = objectUrl;
    link.download = fileName;
    link.style.display = "none";
    document.body.appendChild(link);
    link.click();
  } finally {
    if (link.parentNode) {
      link.parentNode.removeChild(link);
    }
    window.URL.revokeObjectURL(objectUrl);
  }
}
