import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createCustomerAccount,
  deleteCustomerAccount,
  fetchAccountDetails,
  fetchAccounts,
  updateCustomerAccount,
  type BankAccount,
  type CreateCustomerAccountInput,
  type UpdateCustomerAccountInput
} from "../services/accounts";
import { getNormalizedTokenRole } from "../services/session";
import { formatCurrency } from "../utils/formatting";

export function AccountManagementPage() {
  const queryClient = useQueryClient();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";

  const [customerScopeId, setCustomerScopeId] = useState("");
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [feedback, setFeedback] = useState("Create, retrieve, list, update, and delete accounts from this workspace.");

  const [openForm, setOpenForm] = useState<CreateCustomerAccountInput>({
    accountType: "CHECKING",
    currencyCode: "USD",
    nickname: ""
  });

  const [updateForm, setUpdateForm] = useState<Omit<UpdateCustomerAccountInput, "accountId">>({
    nickname: "",
    status: undefined
  });

  const accountsQuery = useQuery({
    queryKey: ["accounts", "list", isAdmin ? customerScopeId : "self"],
    queryFn: () => fetchAccounts(isAdmin ? customerScopeId || undefined : undefined),
    enabled: !isAdmin || Boolean(customerScopeId.trim())
  });

  const selectedAccountDetailsQuery = useQuery({
    queryKey: ["accounts", "details", selectedAccountId],
    queryFn: () => fetchAccountDetails(selectedAccountId),
    enabled: Boolean(selectedAccountId)
  });

  const createMutation = useMutation({
    mutationFn: createCustomerAccount,
    onSuccess: async (account) => {
      setFeedback(`Account created: ${account.accountName} (${account.accountNumberMasked}).`);
      setOpenForm((previous) => ({ ...previous, nickname: "" }));
      await queryClient.invalidateQueries({ queryKey: ["accounts", "list"] });
    },
    onError: (error) => {
      setFeedback(`Unable to create account: ${(error as Error).message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: updateCustomerAccount,
    onSuccess: async (account) => {
      setFeedback(`Account updated: ${account.accountName}.`);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["accounts", "list"] }),
        queryClient.invalidateQueries({ queryKey: ["accounts", "details", account.accountId] })
      ]);
    },
    onError: (error) => {
      setFeedback(`Unable to update account: ${(error as Error).message}`);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCustomerAccount,
    onSuccess: async (result) => {
      setFeedback(`Account ${result.status.toLowerCase()}: ${result.message}`);
      setSelectedAccountId("");
      await queryClient.invalidateQueries({ queryKey: ["accounts", "list"] });
    },
    onError: (error) => {
      setFeedback(`Unable to delete account: ${(error as Error).message}`);
    }
  });

  const onCreateAccount = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedCurrency = openForm.currencyCode.trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(normalizedCurrency)) {
      setFeedback("Currency code must be a valid 3-letter code.");
      return;
    }

    if (isAdmin && !customerScopeId.trim()) {
      setFeedback("Enter customer ID scope before creating accounts as admin.");
      return;
    }

    createMutation.mutate({
      ...openForm,
      currencyCode: normalizedCurrency,
      customerId: isAdmin ? customerScopeId.trim() : undefined
    });
  };

  const onUpdateAccount = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedAccountId) {
      setFeedback("Select an account before updating.");
      return;
    }

    if (!updateForm.nickname && !updateForm.status) {
      setFeedback("Provide nickname or status to update account.");
      return;
    }

    updateMutation.mutate({
      accountId: selectedAccountId,
      nickname: updateForm.nickname?.trim() || undefined,
      status: updateForm.status
    });
  };

  const onDeleteAccount = () => {
    if (!selectedAccountId) {
      setFeedback("Select an account before deleting.");
      return;
    }

    deleteMutation.mutate(selectedAccountId);
  };

  const accounts = accountsQuery.data ?? [];
  const selectedDetails = selectedAccountDetailsQuery.data;

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

            <div className="actions">
              <button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Creating..." : "Create account"}
              </button>
            </div>
          </form>
        </article>

        <article className="surface-card">
          <h3>Retrieve account details</h3>
          <form className="form" onSubmit={(event) => event.preventDefault()}>
            <label>
              Account ID
              <input
                value={selectedAccountId}
                onChange={(event) => setSelectedAccountId(event.target.value)}
                placeholder="account UUID"
              />
            </label>
          </form>

          {!selectedAccountId ? (
            <p className="hint-text">Select an account ID to retrieve details.</p>
          ) : selectedAccountDetailsQuery.isLoading ? (
            <p className="hint-text">Loading account details...</p>
          ) : selectedAccountDetailsQuery.isError ? (
            <p className="hint-text">{(selectedAccountDetailsQuery.error as Error).message}</p>
          ) : selectedDetails ? (
            <AccountDetailsSummary account={selectedDetails} />
          ) : null}
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Update account</h3>
          <form className="form" onSubmit={onUpdateAccount}>
            <label>
              Nickname
              <input
                value={updateForm.nickname ?? ""}
                onChange={(event) =>
                  setUpdateForm((previous) => ({
                    ...previous,
                    nickname: event.target.value
                  }))
                }
                placeholder="New nickname"
              />
            </label>

            <label>
              Status
              <select
                value={updateForm.status ?? ""}
                onChange={(event) =>
                  setUpdateForm((previous) => ({
                    ...previous,
                    status: event.target.value
                      ? (event.target.value as "ACTIVE" | "SUSPENDED" | "CLOSED")
                      : undefined
                  }))
                }
              >
                <option value="">No change</option>
                <option value="ACTIVE">Active</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="CLOSED">Closed</option>
              </select>
            </label>

            <div className="actions">
              <button type="submit" disabled={updateMutation.isPending || !selectedAccountId}>
                {updateMutation.isPending ? "Updating..." : "Update account"}
              </button>
              <button
                type="button"
                className="button-secondary"
                onClick={onDeleteAccount}
                disabled={deleteMutation.isPending || !selectedAccountId}
              >
                {deleteMutation.isPending ? "Deleting..." : "Delete account"}
              </button>
            </div>
          </form>
        </article>

        <article className="surface-card">
          <h3>List customer accounts</h3>
          {accountsQuery.isLoading ? (
            <p className="hint-text">Loading accounts...</p>
          ) : accountsQuery.isError ? (
            <p className="hint-text">Unable to list accounts: {(accountsQuery.error as Error).message}</p>
          ) : accounts.length === 0 ? (
            <p className="hint-text">No accounts found for the selected scope.</p>
          ) : (
            <ul className="stack-list">
              {accounts.map((account) => (
                <li className="stack-list-item" key={account.accountId}>
                  <div>
                    <p className="item-title">{account.accountName}</p>
                    <p className="item-meta">{account.accountType} · {account.accountNumberMasked}</p>
                    <p className="item-meta">ID: {account.accountId}</p>
                  </div>
                  <div className="stack-list-meta">
                    <p className="item-emphasis">{formatCurrency(account.availableBalance, account.currency)}</p>
                    <span className={account.status === "Active" ? "status-pill status-pill--ok" : "status-pill"}>
                      {account.status}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </article>
      </section>

      <article className="surface-card">
        <h3>Operation status</h3>
        <p className="hint-text">{feedback}</p>
      </article>
    </section>
  );
}

function AccountDetailsSummary({ account }: { account: BankAccount }) {
  return (
    <dl className="profile-grid">
      <div>
        <dt>Account ID</dt>
        <dd>{account.accountId}</dd>
      </div>
      <div>
        <dt>Name</dt>
        <dd>{account.accountName}</dd>
      </div>
      <div>
        <dt>Type</dt>
        <dd>{account.accountType}</dd>
      </div>
      <div>
        <dt>Number</dt>
        <dd>{account.accountNumberMasked}</dd>
      </div>
      <div>
        <dt>Available</dt>
        <dd>{formatCurrency(account.availableBalance, account.currency)}</dd>
      </div>
      <div>
        <dt>Current</dt>
        <dd>{formatCurrency(account.currentBalance, account.currency)}</dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd>{account.status}</dd>
      </div>
    </dl>
  );
}
