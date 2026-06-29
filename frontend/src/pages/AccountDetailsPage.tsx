import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  deleteCustomerAccount,
  fetchAccountDetails,
  updateCustomerAccount,
  type BankAccount,
  type UpdateCustomerAccountInput
} from "../services/accounts";
import { fetchCustomerDetails } from "../services/customers";
import { formatCurrency } from "../utils/formatting";

type AccountUpdateForm = Omit<UpdateCustomerAccountInput, "accountId">;

const initialUpdateForm: AccountUpdateForm = {
  nickname: "",
  status: undefined
};

export function AccountDetailsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { accountId: routeAccountId = "" } = useParams<{ accountId: string }>();

  const accountId = routeAccountId.trim();
  const customerScopeId = searchParams.get("customerId")?.trim() ?? "";
  const isAdminPath = location.pathname.startsWith("/admin/");

  const backPath = useMemo(() => {
    if (!isAdminPath) {
      return "/customer/accounts";
    }

    return customerScopeId
      ? `/admin/accounts?customerId=${encodeURIComponent(customerScopeId)}`
      : "/admin/accounts";
  }, [customerScopeId, isAdminPath]);

  const [updateForm, setUpdateForm] = useState<AccountUpdateForm>(initialUpdateForm);
  const [feedback, setFeedback] = useState("Review account information and submit updates from this page.");
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const detailsQuery = useQuery({
    queryKey: ["accounts", "details", accountId],
    queryFn: () => fetchAccountDetails(accountId),
    enabled: Boolean(accountId)
  });

  const associatedCustomerQuery = useQuery({
    queryKey: ["accounts", "associated-customer", customerScopeId],
    queryFn: () => fetchCustomerDetails(customerScopeId),
    enabled: isAdminPath && Boolean(customerScopeId)
  });

  const updateMutation = useMutation({
    mutationFn: updateCustomerAccount,
    onSuccess: async (account) => {
      setUpdateError(null);
      setFeedback(`Account updated: ${account.accountName} (${account.accountId}).`);
      setUpdateForm(initialUpdateForm);

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["accounts", "list"] }),
        queryClient.invalidateQueries({ queryKey: ["accounts", "details", account.accountId] }),
        queryClient.invalidateQueries({ queryKey: ["admin-dashboard", "accounts"] }),
        queryClient.invalidateQueries({ queryKey: ["admin-dashboard", "account-details", account.accountId] })
      ]);
    },
    onError: (error) => {
      setUpdateError(`Unable to update account: ${(error as Error).message}`);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCustomerAccount,
    onSuccess: (result) => {
      setDeleteError(null);
      setFeedback(`Account ${result.status.toLowerCase()}: ${result.message}`);

      navigate(backPath, { replace: true });

      void Promise.all([
        queryClient.invalidateQueries({ queryKey: ["accounts", "list"] }),
        queryClient.invalidateQueries({ queryKey: ["accounts", "details", accountId] }),
        queryClient.invalidateQueries({ queryKey: ["admin-dashboard", "accounts"] }),
        queryClient.invalidateQueries({ queryKey: ["admin-dashboard", "account-details", accountId] })
      ]);
    },
    onError: (error) => {
      setDeleteError(`Unable to delete account: ${(error as Error).message}`);
    }
  });

  const onUpdateAccount = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accountId) {
      setUpdateError("A valid account ID is required to update this account.");
      return;
    }

    const nickname = updateForm.nickname?.trim() || undefined;
    const status = updateForm.status;

    if (!nickname && !status) {
      setUpdateError("Provide nickname or status to update this account.");
      return;
    }

    setUpdateError(null);

    updateMutation.mutate({
      accountId,
      nickname,
      status
    });
  };

  const executeDeleteAccount = () => {
    if (!accountId) {
      setDeleteError("A valid account ID is required to delete this account.");
      return;
    }

    setDeleteError(null);

    deleteMutation.mutate(accountId);
  };

  const onDeleteAccount = () => {
    setDeleteError(null);

    if (isAdminPath) {
      executeDeleteAccount();
      return;
    }

    setIsDeleteConfirmOpen(true);
  };

  const onCancelDeleteAccount = () => {
    if (deleteMutation.isPending) {
      return;
    }
    setIsDeleteConfirmOpen(false);
  };

  const onConfirmDeleteAccount = () => {
    setIsDeleteConfirmOpen(false);
    executeDeleteAccount();
  };

  const account = detailsQuery.data;

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Account details</h2>
          <p className="page-subtitle">View account-specific details and apply updates from one page.</p>
        </div>
      </header>

      <article className="surface-card">
        <div className="actions">
          <button
            type="button"
            className="button-secondary"
            onClick={() => navigate(backPath)}
          >
            Back to accounts
          </button>
        </div>
      </article>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Selected account</h3>
          {!accountId ? (
            <p className="hint-text">No account ID was provided in the route.</p>
          ) : detailsQuery.isLoading ? (
            <p className="hint-text">Loading account details...</p>
          ) : detailsQuery.isError ? (
            <p className="hint-text">{(detailsQuery.error as Error).message}</p>
          ) : account ? (
            <>
              <AccountDetailsSummary account={account} />

              {isAdminPath ? (
                <dl className="profile-grid" style={{ marginTop: "0.62rem" }}>
                  <div>
                    <dt>Associated customer email</dt>
                    <dd>
                      {!customerScopeId
                        ? "Unavailable: missing customer scope"
                        : associatedCustomerQuery.isLoading
                          ? "Loading..."
                          : associatedCustomerQuery.isError
                            ? `Unavailable: ${(associatedCustomerQuery.error as Error).message}`
                            : associatedCustomerQuery.data?.email || "Not available"}
                    </dd>
                  </div>
                </dl>
              ) : null}
            </>
          ) : (
            <p className="hint-text">Account details are unavailable for this account ID.</p>
          )}
        </article>

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
              <button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending ? "Updating..." : "Update account"}
              </button>
              <button
                type="button"
                className="button-secondary"
                onClick={onDeleteAccount}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? "Deleting..." : "Delete account"}
              </button>
            </div>
            {updateError ? <p className="inline-error" role="alert">{updateError}</p> : null}
            {deleteError ? <p className="inline-error" role="alert">{deleteError}</p> : null}
          </form>
        </article>
      </section>

      <article className="surface-card">
        <h3>Operation status</h3>
        <p className="hint-text">{feedback}</p>
      </article>

      {!isAdminPath && isDeleteConfirmOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={onCancelDeleteAccount}>
          <div
            className="confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-account-confirm-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h3 id="delete-account-confirm-title">Confirm account deletion</h3>
            <p>Are you sure you want to delete this account?</p>
            <p>This action closes account access from your profile and cannot be undone.</p>
            <div className="actions">
              <button
                type="button"
                className="button-secondary"
                onClick={onCancelDeleteAccount}
                disabled={deleteMutation.isPending}
              >
                Cancel
              </button>
              <button
                type="button"
                className="button-danger"
                onClick={onConfirmDeleteAccount}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? "Deleting..." : "Yes, delete this account"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
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
