import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  fetchStatement,
  fetchStatementPdf,
  fetchStatementTransactions,
  type StatementDetail
} from "../services/statements";
import { getNormalizedTokenRole } from "../services/session";
import type { TransactionItem } from "../services/transactions";
import { formatCurrency, formatDate, formatDateTime, formatStatementPeriod } from "../utils/formatting";

type StatementLedgerRow = TransactionItem & {
  debitAmount: number | null;
  creditAmount: number | null;
  runningBalance: number;
};

type StatementLedgerModel = {
  openingBalance: number;
  closingBalance: number;
  rows: StatementLedgerRow[];
};

export function StatementDetailsPage() {
  const { statementId: routeStatementId } = useParams<{ statementId: string }>();
  const statementId = routeStatementId?.trim() ?? "";
  const [searchParams] = useSearchParams();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";
  const baseStatementsPath = isAdmin ? "/admin/statements" : "/customer/statements";
  const adminScopeId = searchParams.get("customerId")?.trim() ?? "";
  const backLink = isAdmin && adminScopeId
    ? `${baseStatementsPath}?customerId=${encodeURIComponent(adminScopeId)}`
    : baseStatementsPath;

  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState("Review monthly statement details and download the statement PDF artifact.");

  const statementDetailQuery = useQuery({
    queryKey: ["statements", "detail", statementId],
    queryFn: () => fetchStatement(statementId),
    enabled: Boolean(statementId)
  });

  const statementTransactionsQuery = useQuery({
    queryKey: ["statements", "detail", "transactions", statementId],
    queryFn: () =>
      fetchStatementTransactions({
        accountId: statementDetailQuery.data?.accountId ?? "",
        periodYearMonth: statementDetailQuery.data?.periodYearMonth ?? ""
      }),
    enabled: Boolean(statementDetailQuery.data?.statementId)
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

  const statementTransactions = statementTransactionsQuery.data ?? [];

  const ledger = useMemo<StatementLedgerModel>(() => {
    const defaultOpeningBalance = statementDetailQuery.data?.openingBalance ?? 0;
    const defaultClosingBalance = statementDetailQuery.data?.closingBalance ?? defaultOpeningBalance;

    if (!statementDetailQuery.data || statementTransactions.length === 0) {
      return {
        openingBalance: defaultOpeningBalance,
        closingBalance: defaultClosingBalance,
        rows: []
      };
    }

    const ordered = [...statementTransactions].sort((left, right) => {
      const leftTime = Date.parse(left.bookedAt);
      const rightTime = Date.parse(right.bookedAt);
      if (leftTime !== rightTime) {
        return leftTime - rightTime;
      }

      return left.transactionId.localeCompare(right.transactionId);
    });

    const firstTransaction = ordered[0];
    const firstMovement = firstTransaction.direction === "CREDIT"
      ? firstTransaction.amount
      : -firstTransaction.amount;

    const openingBalance = Number.isFinite(firstTransaction.balanceAfter)
      ? roundCurrency((firstTransaction.balanceAfter as number) - firstMovement)
      : defaultOpeningBalance;

    let runningBalance = openingBalance;
    const rows = ordered.map((item) => {
      const movement = item.direction === "CREDIT" ? item.amount : -item.amount;

      if (Number.isFinite(item.balanceAfter)) {
        runningBalance = roundCurrency(item.balanceAfter as number);
      } else {
        runningBalance = roundCurrency(runningBalance + movement);
      }

      return {
        ...item,
        debitAmount: item.direction === "DEBIT" ? item.amount : null,
        creditAmount: item.direction === "CREDIT" ? item.amount : null,
        runningBalance
      };
    });

    const closingBalance = rows.length > 0
      ? rows[rows.length - 1].runningBalance
      : defaultClosingBalance;

    return {
      openingBalance,
      closingBalance,
      rows
    };
  }, [statementDetailQuery.data, statementTransactions]);

  const ledgerRows = ledger.rows;

  const onDownloadPdf = () => {
    if (!statementDetailQuery.data) {
      return;
    }

    downloadPdfMutation.mutate(statementDetailQuery.data);
  };

  if (!statementId) {
    return (
      <section className="bank-page">
        <header className="page-header">
          <div>
            <h2 className="page-title">Statement details</h2>
            <p className="page-subtitle">Open statement details from the statements workspace.</p>
          </div>
          <div className="actions">
            <Link className="admin-customer-header-link" to={backLink}>
              Back to statements
            </Link>
          </div>
        </header>

        <article className="surface-card">
          <p className="hint-text">No statement ID was provided in the route.</p>
        </article>
      </section>
    );
  }

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Statement details</h2>
          <p className="page-subtitle">Review balances and posted activity for the selected monthly statement.</p>
        </div>
        <div className="actions">
          <Link className="admin-customer-header-link" to={backLink}>
            Back to statements
          </Link>
        </div>
      </header>

      <p className="output">{feedback}</p>

      <article className="surface-card">
        <h3>Selected statement summary</h3>
        {statementDetailQuery.isPending ? (
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
                <dd>{formatStatementPeriod(statementDetailQuery.data.periodYearMonth)}</dd>
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
          </>
        ) : null}
      </article>

      <article className="surface-card">
        <h3>Statement transactions</h3>
        {statementTransactionsQuery.isPending ? (
          <p className="hint-text">Loading statement transactions...</p>
        ) : null}
        {statementTransactionsQuery.isError ? (
          <p className="hint-text">Unable to load statement transactions: {(statementTransactionsQuery.error as Error).message}</p>
        ) : null}
        {!statementTransactionsQuery.isPending && !statementTransactionsQuery.isError && statementTransactions.length === 0 ? (
          <p className="hint-text">No transactions were posted in this statement period.</p>
        ) : null}
        {ledgerRows.length > 0 && statementDetailQuery.data ? (
          <div className="statement-table-shell statement-table-shell--ledger">
            <table className="statement-table statement-table--ledger" aria-label="Statement transactions table">
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Description</th>
                  <th scope="col">Type</th>
                  <th scope="col" className="statement-table-number">Debit</th>
                  <th scope="col" className="statement-table-number">Credit</th>
                  <th scope="col" className="statement-table-number">Running balance</th>
                </tr>
              </thead>
              <tbody>
                <tr className="statement-opening-row">
                  <td data-label="Date">{formatDate(`${statementDetailQuery.data.periodYearMonth}-01T00:00:00Z`)}</td>
                  <td data-label="Description">Opening balance</td>
                  <td data-label="Type">Balance forward</td>
                  <td data-label="Debit" className="statement-table-number">-</td>
                  <td data-label="Credit" className="statement-table-number">-</td>
                  <td data-label="Running balance" className="statement-table-number">
                    {formatCurrency(ledger.openingBalance, statementDetailQuery.data.currencyCode)}
                  </td>
                </tr>
                {ledgerRows.map((item) => (
                  <tr key={item.transactionId}>
                    <td data-label="Date">{formatDate(item.bookedAt)}</td>
                    <td data-label="Description">
                      <p className="item-title">{item.description || "Transaction"}</p>
                      <p className="statement-table-meta">Ref {item.transactionId}</p>
                    </td>
                    <td data-label="Type">{toReadableStatementTransactionType(item)}</td>
                    <td data-label="Debit" className="statement-table-number statement-table-number--debit">
                      {item.debitAmount === null ? "-" : formatCurrency(item.debitAmount, item.currency)}
                    </td>
                    <td data-label="Credit" className="statement-table-number statement-table-number--credit">
                      {item.creditAmount === null ? "-" : formatCurrency(item.creditAmount, item.currency)}
                    </td>
                    <td data-label="Running balance" className="statement-table-number">
                      {formatCurrency(item.runningBalance, item.currency)}
                    </td>
                  </tr>
                ))}
                <tr className="statement-closing-row">
                  <td data-label="Date">-</td>
                  <td data-label="Description">Closing balance</td>
                  <td data-label="Type">Balance carry</td>
                  <td data-label="Debit" className="statement-table-number">-</td>
                  <td data-label="Credit" className="statement-table-number">-</td>
                  <td data-label="Running balance" className="statement-table-number">
                    {formatCurrency(ledger.closingBalance, statementDetailQuery.data.currencyCode)}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        ) : null}
      </article>
    </section>
  );
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

function roundCurrency(value: number): number {
  return Math.round(value * 100) / 100;
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
