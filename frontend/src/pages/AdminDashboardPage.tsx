import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchAccountDetails,
  fetchAccounts,
  updateCustomerAccount,
  type BankAccount,
  type UpdateCustomerAccountInput
} from "../services/accounts";
import { fetchCustomersForAdmin, type CustomerListItem } from "../services/customers";
import { getTokenEmail, getTokenSubject, setActiveCustomerId } from "../services/session";
import { formatCurrency, formatDate } from "../utils/formatting";

type AccountUpdateForm = Omit<UpdateCustomerAccountInput, "accountId">;

const initialAccountUpdateForm: AccountUpdateForm = {
  nickname: "",
  status: undefined
};

export function AdminDashboardPage() {
  const queryClient = useQueryClient();
  const signedInEmail = getTokenEmail() ?? "Not available";
  const signedInUserId = getTokenSubject() ?? "Not available";

  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [updateForm, setUpdateForm] = useState<AccountUpdateForm>(initialAccountUpdateForm);
  const [feedback, setFeedback] = useState(
    "Select a customer to review and update that customer's checking/savings accounts."
  );

  const customersQuery = useQuery({
    queryKey: ["admin-dashboard", "customers"],
    queryFn: () => fetchCustomersForAdmin(1, 200)
  });

  const customers = customersQuery.data ?? [];

  useEffect(() => {
    if (!selectedCustomerId && customers.length > 0) {
      const firstCustomerId = customers[0].customerId;
      setSelectedCustomerId(firstCustomerId);
      setActiveCustomerId(firstCustomerId);
    }
  }, [customers, selectedCustomerId]);

  const accountsQuery = useQuery({
    queryKey: ["admin-dashboard", "accounts", selectedCustomerId],
    queryFn: () => fetchAccounts(selectedCustomerId),
    enabled: Boolean(selectedCustomerId)
  });

  const accounts = accountsQuery.data ?? [];

  useEffect(() => {
    setSelectedAccountId("");
    setUpdateForm(initialAccountUpdateForm);
  }, [selectedCustomerId]);

  useEffect(() => {
    if (accounts.length === 0) {
      setSelectedAccountId("");
      return;
    }

    if (!selectedAccountId || !accounts.some((account) => account.accountId === selectedAccountId)) {
      setSelectedAccountId(accounts[0].accountId);
    }
  }, [accounts, selectedAccountId]);

  const selectedCustomer = useMemo(
    () => customers.find((customer) => customer.customerId === selectedCustomerId) ?? null,
    [customers, selectedCustomerId]
  );

  const selectedAccountDetailsQuery = useQuery({
    queryKey: ["admin-dashboard", "account-details", selectedAccountId],
    queryFn: () => fetchAccountDetails(selectedAccountId),
    enabled: Boolean(selectedAccountId)
  });

  const selectedAccount = selectedAccountDetailsQuery.data
    ?? accounts.find((account) => account.accountId === selectedAccountId)
    ?? null;

  const updateMutation = useMutation({
    mutationFn: updateCustomerAccount,
    onSuccess: async (account) => {
      setFeedback(`Account updated: ${account.accountName} (${account.accountId}).`);
      setUpdateForm(initialAccountUpdateForm);

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin-dashboard", "accounts", selectedCustomerId] }),
        queryClient.invalidateQueries({ queryKey: ["admin-dashboard", "account-details", account.accountId] }),
        queryClient.invalidateQueries({ queryKey: ["accounts", "list", selectedCustomerId] }),
        queryClient.invalidateQueries({ queryKey: ["accounts", "details", account.accountId] })
      ]);
    },
    onError: (error) => {
      setFeedback(`Unable to update account: ${(error as Error).message}`);
    }
  });

  const selectedCustomerBalance = useMemo(
    () => accounts.reduce((sum, account) => sum + account.availableBalance, 0),
    [accounts]
  );

  const onSelectCustomer = (customerId: string) => {
    setSelectedCustomerId(customerId);
    setActiveCustomerId(customerId);
    setFeedback(`Loaded customer scope: ${customerId}.`);
  };

  const onSelectAccount = (accountId: string) => {
    setSelectedAccountId(accountId);
    setFeedback(`Selected account ${accountId} for update.`);
  };

  const onUpdateAccount = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedAccount) {
      setFeedback("Select an account before updating.");
      return;
    }

    const nickname = updateForm.nickname?.trim() || undefined;
    const status = updateForm.status;

    if (!nickname && !status) {
      setFeedback("Provide nickname or status to update the selected account.");
      return;
    }

    updateMutation.mutate({
      accountId: selectedAccount.accountId,
      nickname,
      status
    });
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Admin workspace</h2>
          <p className="page-subtitle">
            View all customers, select one, and manage that customer's checking/savings accounts.
          </p>
        </div>
      </header>

      <section className="summary-grid" aria-label="Admin customer overview">
        <article className="summary-card">
          <p className="summary-label">Total customers</p>
          <p className="summary-value">{customers.length}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Selected customer accounts</p>
          <p className="summary-value">{accounts.length}</p>
        </article>
        <article className="summary-card">
          <p className="summary-label">Selected customer balance</p>
          <p className="summary-value">{formatCurrency(selectedCustomerBalance, "USD")}</p>
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>All customers</h3>
          {customersQuery.isLoading ? (
            <p className="hint-text">Loading customer directory...</p>
          ) : customersQuery.isError ? (
            <p className="hint-text">Unable to load customers: {(customersQuery.error as Error).message}</p>
          ) : customers.length === 0 ? (
            <p className="hint-text">No customers found.</p>
          ) : (
            <ul className="stack-list">
              {customers.map((customer) => (
                <li key={customer.customerId}>
                  <button
                    type="button"
                    className={
                      selectedCustomerId === customer.customerId
                        ? "selector-list-button active"
                        : "selector-list-button"
                    }
                    onClick={() => onSelectCustomer(customer.customerId)}
                  >
                    <div>
                      <p className="item-title">{customer.fullName}</p>
                      <p className="item-meta">{customer.email || "No email"}</p>
                      <p className="item-meta">ID: {customer.customerId}</p>
                    </div>
                    <div className="stack-list-meta">
                      <p className="item-emphasis">{customer.externalCustomerKey || "No key"}</p>
                      <span className={customer.status === "ACTIVE" ? "status-pill status-pill--ok" : "status-pill"}>
                        {customer.status}
                      </span>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="surface-card">
          <h3>Selected customer profile</h3>
          {!selectedCustomer ? (
            <p className="hint-text">Select a customer to view profile details.</p>
          ) : (
            <dl className="profile-grid">
              <div>
                <dt>Customer ID</dt>
                <dd>{selectedCustomer.customerId}</dd>
              </div>
              <div>
                <dt>Name</dt>
                <dd>{selectedCustomer.fullName}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{selectedCustomer.email || "-"}</dd>
              </div>
              <div>
                <dt>Mobile</dt>
                <dd>{selectedCustomer.mobile || "-"}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{selectedCustomer.status}</dd>
              </div>
              <div>
                <dt>Joined</dt>
                <dd>{formatDate(selectedCustomer.joinedAt)}</dd>
              </div>
            </dl>
          )}

          <h3 style={{ marginTop: "0.92rem" }}>Current admin session</h3>
          <dl className="profile-grid">
            <div>
              <dt>Email</dt>
              <dd>{signedInEmail}</dd>
            </div>
            <div>
              <dt>User ID</dt>
              <dd>{signedInUserId}</dd>
            </div>
          </dl>
        </article>
      </section>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Checking & savings accounts</h3>
          {!selectedCustomerId ? (
            <p className="hint-text">Select a customer to view accounts.</p>
          ) : accountsQuery.isLoading ? (
            <p className="hint-text">Loading customer accounts...</p>
          ) : accountsQuery.isError ? (
            <p className="hint-text">Unable to load accounts: {(accountsQuery.error as Error).message}</p>
          ) : accounts.length === 0 ? (
            <p className="hint-text">No checking/savings accounts found for this customer.</p>
          ) : (
            <ul className="stack-list">
              {accounts.map((account) => (
                <li key={account.accountId}>
                  <button
                    type="button"
                    className={
                      selectedAccountId === account.accountId
                        ? "selector-list-button active"
                        : "selector-list-button"
                    }
                    onClick={() => onSelectAccount(account.accountId)}
                  >
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
                  </button>
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="surface-card">
          <h3>Update selected account</h3>
          {!selectedAccount ? (
            <p className="hint-text">Select a customer account to edit nickname or status.</p>
          ) : (
            <>
              <AccountDetailsSummary account={selectedAccount} />

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
                  <button type="submit" disabled={updateMutation.isPending}>
                    {updateMutation.isPending ? "Updating..." : "Update account"}
                  </button>
                </div>
              </form>
            </>
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
