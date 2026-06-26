import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  createCustomerAccount,
  fetchAccounts,
  type CreateCustomerAccountInput
} from "../services/accounts";
import {
  getActiveCustomerId,
  getTokenEmail,
  getTokenRole,
  getTokenSubject
} from "../services/session";
import { formatCurrency } from "../utils/formatting";

export function AccountManagementPage() {
  const queryClient = useQueryClient();
  const accountsQuery = useQuery({
    queryKey: ["accounts"],
    queryFn: fetchAccounts
  });

  const [openForm, setOpenForm] = useState<CreateCustomerAccountInput>({
    accountType: "CHECKING",
    currencyCode: "USD",
    nickname: ""
  });
  const [feedback, setFeedback] = useState("Open a new checking or savings account.");
  const [needsCustomerProfile, setNeedsCustomerProfile] = useState(false);

  const signedInEmail = getTokenEmail() ?? "Unknown";
  const signedInUserId = getTokenSubject() ?? "Unknown";
  const signedInRole = getTokenRole() ?? "Unknown";
  const activeCustomerId = getActiveCustomerId() ?? "Not set";

  const openAccountMutation = useMutation({
    mutationFn: createCustomerAccount,
    onSuccess: async (account) => {
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      setOpenForm((previous) => ({ ...previous, nickname: "" }));
      setNeedsCustomerProfile(false);
      setFeedback(`Account opened successfully: ${account.accountName}.`);
    },
    onError: (error) => {
      const message = (error as Error).message;
      setNeedsCustomerProfile(message.toLowerCase().includes("customer profile"));
      setFeedback(`Unable to open account: ${message}`);
    }
  });

  const accounts = accountsQuery.data ?? [];
  const accountsLoadError = accountsQuery.isError ? (accountsQuery.error as Error).message : "";

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedCurrency = openForm.currencyCode.trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(normalizedCurrency)) {
      setNeedsCustomerProfile(false);
      setFeedback("Currency code must be a 3-letter code such as USD.");
      return;
    }

    try {
      await openAccountMutation.mutateAsync({
        ...openForm,
        currencyCode: normalizedCurrency
      });
    } catch {
      // Feedback is set in mutation onError.
    }
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Accounts</h2>
          <p className="page-subtitle">Track balances and stay on top of account health.</p>
        </div>
      </header>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Open a new account</h3>
          <form className="form" onSubmit={onSubmit}>
            <label>
              Account type
              <select
                value={openForm.accountType}
                onChange={(event) =>
                  setOpenForm({
                    ...openForm,
                    accountType: event.target.value as CreateCustomerAccountInput["accountType"]
                  })
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
                  setOpenForm({
                    ...openForm,
                    currencyCode: event.target.value
                  })
                }
                placeholder="USD"
                maxLength={3}
                required
              />
            </label>

            <label>
              Nickname (optional)
              <input
                value={openForm.nickname ?? ""}
                onChange={(event) =>
                  setOpenForm({
                    ...openForm,
                    nickname: event.target.value
                  })
                }
                placeholder="Everyday Spending"
                maxLength={64}
              />
            </label>

            <div className="actions">
              <button type="submit" disabled={openAccountMutation.isPending}>
                {openAccountMutation.isPending ? "Opening..." : "Open account"}
              </button>
            </div>
          </form>
          <p className="hint-text">{feedback}</p>
        </article>

        <article className="surface-card">
          <h3>Customer account status</h3>
          <dl className="profile-grid">
            <div>
              <dt>Signed-in email</dt>
              <dd>{signedInEmail}</dd>
            </div>
            <div>
              <dt>Signed-in user ID</dt>
              <dd>{signedInUserId}</dd>
            </div>
            <div>
              <dt>Role</dt>
              <dd>{signedInRole}</dd>
            </div>
            <div>
              <dt>Active customer ID</dt>
              <dd>{activeCustomerId}</dd>
            </div>
          </dl>
          {accountsQuery.isPending ? (
            <p className="hint-text">Checking current customer accounts...</p>
          ) : accountsQuery.isError ? (
            <p className="hint-text">
              {accountsLoadError.includes("No customer context found")
                ? "No customer context found. Sign in and try again."
                : `Unable to read customer accounts: ${accountsLoadError}`}
            </p>
          ) : accounts.length === 0 ? (
            <p className="hint-text">No accounts found for customer.</p>
          ) : (
            <p className="hint-text">{accounts.length} account(s) found for customer.</p>
          )}
        </article>
      </section>

      {needsCustomerProfile ? (
        <article className="surface-card">
          <p className="hint-text">
            No customer profile is linked to this signed-in identity. Continue with
            {" "}
            <Link to="/security/create-customer">Create customer profile</Link>
            {" "}
            and then try opening the account again.
          </p>
        </article>
      ) : null}

      {accountsQuery.isPending ? (
        <article className="surface-card">
          <p className="hint-text">Loading account information...</p>
        </article>
      ) : accountsQuery.isError ? (
        <article className="surface-card">
          <p className="hint-text">
            {accountsLoadError.includes("No customer context found")
              ? "No customer context found. Sign in and open this page again."
              : `Unable to load accounts: ${accountsLoadError}`}
          </p>
        </article>
      ) : accounts.length === 0 ? (
        <article className="surface-card">
          <p className="hint-text">No accounts found for customer.</p>
        </article>
      ) : (
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
      )}
    </section>
  );
}
