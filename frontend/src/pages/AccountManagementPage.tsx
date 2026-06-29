import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  createCustomerAccount,
  fetchAccounts,
  type CreateCustomerAccountInput
} from "../services/accounts";
import { getNormalizedTokenRole } from "../services/session";
import { formatCurrency } from "../utils/formatting";

export function AccountManagementPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";
  const initialScopeId = isAdmin ? (searchParams.get("customerId") ?? "") : "";

  const [customerScopeId, setCustomerScopeId] = useState(initialScopeId);
  const [feedback, setFeedback] = useState(
    "Create and list accounts in this workspace, then open a specific account to view and update it."
  );
  const [createError, setCreateError] = useState<string | null>(null);

  const [openForm, setOpenForm] = useState<CreateCustomerAccountInput>({
    accountType: "CHECKING",
    currencyCode: "USD",
    nickname: "",
    interestRate: 0
  });

  const accountsQuery = useQuery({
    queryKey: ["accounts", "list", isAdmin ? customerScopeId : "self"],
    queryFn: () => fetchAccounts(isAdmin ? customerScopeId || undefined : undefined),
    enabled: !isAdmin || Boolean(customerScopeId.trim())
  });

  const createMutation = useMutation({
    mutationFn: createCustomerAccount,
    onSuccess: async (account) => {
      setCreateError(null);
      setFeedback(`Account created: ${account.accountName} (${account.accountNumberMasked}).`);
      setOpenForm((previous) => ({ ...previous, nickname: "" }));
      await queryClient.invalidateQueries({ queryKey: ["accounts", "list"] });
    },
    onError: (error) => {
      setCreateError(`Unable to create account: ${(error as Error).message}`);
    }
  });

  const onCreateAccount = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedCurrency = openForm.currencyCode.trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(normalizedCurrency)) {
      setCreateError("Currency code must be a valid 3-letter code.");
      return;
    }

    if (isAdmin && !customerScopeId.trim()) {
      setCreateError("Enter customer ID scope before creating accounts as admin.");
      return;
    }

    if (openForm.accountType === "SAVINGS") {
      const interestRate = typeof openForm.interestRate === "number" ? openForm.interestRate : Number.NaN;
      if (!Number.isFinite(interestRate) || interestRate < 0) {
        setCreateError("Interest rate must be a non-negative number.");
        return;
      }
    }

    setCreateError(null);

    createMutation.mutate({
      ...openForm,
      currencyCode: normalizedCurrency,
      customerId: isAdmin ? customerScopeId.trim() : undefined
    });
  };

  const onOpenAccount = (accountId: string) => {
    const basePath = isAdmin ? "/admin/accounts" : "/customer/accounts";
    const scopeId = customerScopeId.trim();
    const query = isAdmin && scopeId ? `?customerId=${encodeURIComponent(scopeId)}` : "";
    navigate(`${basePath}/${encodeURIComponent(accountId)}${query}`);
  };

  const accounts = accountsQuery.data ?? [];

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Account management</h2>
          <p className="page-subtitle">Create, retrieve, list, update, and delete checking/savings accounts.</p>
        </div>
      </header>

      {isAdmin ? (
        <article className="surface-card">
          <h3>Admin scope</h3>
          <form className="form" onSubmit={(event) => event.preventDefault()}>
            <label>
              Target customer ID
              <input
                value={customerScopeId}
                onChange={(event) => setCustomerScopeId(event.target.value)}
                placeholder="customer UUID"
              />
            </label>
          </form>
          <p className="hint-text">Provide customer ID to manage a specific customer account set.</p>
        </article>
      ) : null}

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Create account</h3>
          <form className="form" onSubmit={onCreateAccount}>
            <label>
              Account type
              <select
                value={openForm.accountType}
                onChange={(event) =>
                  setOpenForm((previous) => ({
                    ...previous,
                    accountType: event.target.value as CreateCustomerAccountInput["accountType"]
                  }))
                }
              >
                <option value="CHECKING">Checking</option>
                <option value="SAVINGS">Savings</option>
              </select>
            </label>

            <label>
              Currency code
              <input
                value={openForm.currencyCode}
                onChange={(event) =>
                  setOpenForm((previous) => ({
                    ...previous,
                    currencyCode: event.target.value
                  }))
                }
                placeholder="USD"
                maxLength={3}
              />
            </label>

            <label>
              Nickname
              <input
                value={openForm.nickname ?? ""}
                onChange={(event) =>
                  setOpenForm((previous) => ({
                    ...previous,
                    nickname: event.target.value
                  }))
                }
                placeholder="Daily Spending"
              />
            </label>

            {openForm.accountType === "SAVINGS" ? (
              <label>
                Interest rate (%)
                <input
                  type="number"
                  min={0}
                  step="0.0001"
                  value={openForm.interestRate ?? 0}
                  onChange={(event) =>
                    setOpenForm((previous) => ({
                      ...previous,
                      interestRate: Number(event.target.value)
                    }))
                  }
                />
              </label>
            ) : null}

            <div className="actions">
              <button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Creating..." : "Create account"}
              </button>
            </div>
            {createError ? <p className="inline-error" role="alert">{createError}</p> : null}
          </form>
        </article>

        <article className="surface-card">
          <h3>Customer accounts</h3>
          {accountsQuery.isLoading ? (
            <p className="hint-text">Loading accounts...</p>
          ) : accountsQuery.isError ? (
            <p className="hint-text">Unable to list accounts: {(accountsQuery.error as Error).message}</p>
          ) : accounts.length === 0 ? (
            <p className="hint-text">No accounts found for the selected scope.</p>
          ) : (
            <ul className="stack-list">
              {accounts.map((account) => (
                <li key={account.accountId}>
                  <button
                    type="button"
                    className="selector-list-button"
                    onClick={() => onOpenAccount(account.accountId)}
                    aria-label={`Open details for ${account.accountName}`}
                  >
                    <div>
                      <p className="item-title">{account.accountName}</p>
                      <p className="item-meta">{account.accountType} · {account.accountNumberMasked}</p>
                      {account.checkingNumber !== null ? (
                        <p className="item-meta">Checking number: {account.checkingNumber}</p>
                      ) : null}
                      {account.accountType === "Savings" ? (
                        <p className="item-meta">Interest rate: {account.interestRate.toFixed(4)}%</p>
                      ) : null}
                      <p className="item-meta">ID: {account.accountId}</p>
                    </div>
                    <div className="stack-list-meta">
                      <p className="item-emphasis">{formatCurrency(account.availableBalance, account.currency)}</p>
                      <span className={account.status === "Active" ? "status-pill status-pill--ok" : "status-pill"}>
                        {account.status}
                      </span>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <p className="hint-text">Click an account to open its dedicated details and update page.</p>
        </article>
      </section>

      <article className="surface-card">
        <h3>Operation status</h3>
        <p className="hint-text">{feedback}</p>
      </article>
    </section>
  );
}
