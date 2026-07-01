import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { fetchAccounts, type BankAccount } from "../services/accounts";
import { fetchCustomersForAdmin } from "../services/customers";
import { getNormalizedTokenRole } from "../services/session";
import {
  fetchStatements,
  generateStatement,
  type StatementGenerationMode,
  type StatementListResult
} from "../services/statements";
import {
  filterCustomersByNameOrId,
  formatCustomerScopeOption,
  resolveCustomerIdFromScopeInput
} from "../utils/customerScope";
import { formatDateTime, formatStatementPeriod } from "../utils/formatting";

const emptyStatements: StatementListResult = {
  items: [],
  page: 1,
  pageSize: 20,
  totalItems: 0,
  totalPages: 1
};

export function StatementsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";
  const initialScopeId = isAdmin ? (searchParams.get("customerId") ?? "") : "";
  const [accountId, setAccountId] = useState("");
  const [periodYearMonth, setPeriodYearMonth] = useState("");
  const [generationMode, setGenerationMode] = useState<StatementGenerationMode>("STANDARD");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [customerScopeInput, setCustomerScopeInput] = useState(initialScopeId);
  const [selectedCustomerScopeId, setSelectedCustomerScopeId] = useState(initialScopeId);
  const [feedback, setFeedback] = useState(
    isAdmin
      ? "Select a customer scope, then generate and retrieve monthly statements for that customer accounts."
      : "Generate and retrieve monthly statements for your accounts."
  );

  const adminCustomersQuery = useQuery({
    queryKey: ["customers", "admin", "statement-scope-options"],
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
    queryKey: ["accounts", "statements", "scope", isAdmin ? customerScopeId || "none" : "self"],
    queryFn: () => fetchAccounts(isAdmin ? customerScopeId || undefined : undefined),
    enabled: !isAdmin || Boolean(customerScopeId.trim())
  });

  const accounts = accountsQuery.data ?? [];
  const hasAccounts = accounts.length > 0;

  useEffect(() => {
    if (isAdmin && !customerScopeId.trim()) {
      setAccountId("");
      setPage(1);
      return;
    }

    if (!hasAccounts) {
      setAccountId("");
      return;
    }

    if (!accountId || !accounts.some((item) => item.accountId === accountId)) {
      setAccountId(accounts[0].accountId);
      setPage(1);
    }
  }, [accounts, hasAccounts, accountId, isAdmin, customerScopeId]);

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

  const generateMutation = useMutation({
    mutationFn: generateStatement,
    onSuccess: async (result) => {
      setFeedback(`Statement generation ${result.generationStatus.toLowerCase()} for request ${result.statementId}.`);
      await queryClient.invalidateQueries({ queryKey: ["statements"] });
    },
    onError: (error) => {
      setFeedback(`Statement generation failed: ${(error as Error).message}`);
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

    if (isAdmin && !customerScopeId.trim()) {
      setFeedback("Select a customer scope before generating statements as admin.");
      return;
    }

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
  };

  const onViewDetails = (statementId: string) => {
    const basePath = isAdmin ? "/admin/statements" : "/customer/statements";
    const query = isAdmin ? location.search : "";
    navigate(`${basePath}/${encodeURIComponent(statementId)}${query}`);
  };

  const hasPreviousPage = statements.page > 1;
  const hasNextPage = statements.page < statements.totalPages;

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

      {isAdmin ? (
        <article className="surface-card">
          <h3>Admin scope</h3>
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
          <p className="hint-text">Provide a customer scope to load and generate monthly statements for that customer accounts.</p>
          {adminCustomersQuery.isError ? (
            <p className="hint-text">Unable to load customer scope options: {(adminCustomersQuery.error as Error).message}</p>
          ) : null}
          {customerScopeInput.trim() && !customerScopeId ? (
            <p className="hint-text">Select a customer from suggestions or provide an exact customer ID.</p>
          ) : null}
        </article>
      ) : null}

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
                }}
                disabled={accountsQuery.isPending || accountsQuery.isError || !hasAccounts || (isAdmin && !customerScopeId.trim())}
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
              <p className="hint-text">
                {isAdmin && !customerScopeId.trim()
                  ? "Select a customer scope and account before generating statements."
                  : "Select an account before generating statements."}
              </p>
            ) : (
              <p className="hint-text">
                Statements are generated for {selectedAccount.accountName}.
              </p>
            )}
          </form>
        </article>
      </section>

      <article className="surface-card">
        <h3>Statement results</h3>
        {statementsQuery.isPending ? <p className="hint-text">Loading statements...</p> : null}
        {statementsQuery.isError ? (
          <p className="hint-text">Unable to load statements: {(statementsQuery.error as Error).message}</p>
        ) : null}
        {!statementsQuery.isPending && !statementsQuery.isError && statements.items.length === 0 ? (
          <p className="hint-text">No statements found for the selected filters.</p>
        ) : null}
        {!statementsQuery.isPending && !statementsQuery.isError && statements.items.length > 0 ? (
          <div className="statement-table-shell">
            <table className="statement-table" aria-label="Statement results table">
              <thead>
                <tr>
                  <th scope="col">Period</th>
                  <th scope="col">Version</th>
                  <th scope="col">Account</th>
                  <th scope="col">Generated</th>
                  <th scope="col">Status</th>
                  <th scope="col">Action</th>
                </tr>
              </thead>
              <tbody>
                {statements.items.map((statement) => (
                  <tr key={statement.statementId}>
                    <td data-label="Period">{formatStatementPeriod(statement.periodYearMonth)}</td>
                    <td data-label="Version">v{statement.artifactVersion}</td>
                    <td data-label="Account">{statement.accountId}</td>
                    <td data-label="Generated">{formatDateTime(statement.generatedAtUtc)}</td>
                    <td data-label="Status">
                      <span className={statement.status === "FAILED" ? "status-pill status-pill--warn" : "status-pill status-pill--ok"}>
                        {statement.status}
                      </span>
                    </td>
                    <td data-label="Action" className="statement-table-actions">
                      <button
                        type="button"
                        className="button-secondary"
                        onClick={() => onViewDetails(statement.statementId)}
                      >
                        View details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

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
    </section>
  );
}

function formatAccountLabel(account: BankAccount): string {
  return `${account.accountName} (${account.accountNumberMasked})`;
}
