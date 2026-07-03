import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createCustomerProfile,
  deleteCustomerProfile,
  fetchCustomersForAdmin,
  fetchCustomerDetails,
  fetchCustomerProfile,
  updateCustomerProfile,
  type CreateCustomerProfileInput,
  type CustomerProfile,
  type UpdateCustomerProfileInput
} from "../services/customers";
import { clearActiveCustomerId, clearAuthSession, getNormalizedTokenRole, setActiveCustomerId } from "../services/session";
import {
  filterCustomersByNameOrId,
  formatCustomerScopeOption,
  resolveCustomerIdFromScopeInput
} from "../utils/customerScope";
import { formatDate } from "../utils/formatting";
import {
  PHONE_NUMBER_MAX_LENGTH,
  validatePhoneNumber
} from "../utils/phoneValidation";
import { useNavigate } from "react-router-dom";

const initialCreate: CreateCustomerProfileInput = {
  externalCustomerKey: "",
  legalName: "",
  primaryEmail: "",
  phoneNumber: "",
  password: ""
};

const initialUpdate: UpdateCustomerProfileInput = {
  legalName: "",
  primaryEmail: "",
  phoneNumber: "",
  status: undefined
};

export function CustomerManagementPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";
  const initialFeedbackMessage = "Retrieve and update customer profiles. Admins can create and delete customers.";

  const [selectedCustomerScopeInput, setSelectedCustomerScopeInput] = useState("");
  const [selectedCustomerScopeId, setSelectedCustomerScopeId] = useState("");
  const [feedback, setFeedback] = useState(initialFeedbackMessage);
  const [createForm, setCreateForm] = useState<CreateCustomerProfileInput>(initialCreate);
  const [updateForm, setUpdateForm] = useState<UpdateCustomerProfileInput>(initialUpdate);
  const [isCloseConfirmOpen, setIsCloseConfirmOpen] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const selfProfileQuery = useQuery({
    queryKey: ["customers", "self"],
    queryFn: fetchCustomerProfile,
    enabled: !isAdmin
  });

  const adminCustomersQuery = useQuery({
    queryKey: ["customers", "admin", "profile-scope-options"],
    queryFn: () => fetchCustomersForAdmin(1, 200),
    enabled: isAdmin
  });

  const adminCustomers = adminCustomersQuery.data ?? [];

  const inferredCustomerId = useMemo(
    () => resolveCustomerIdFromScopeInput(selectedCustomerScopeInput, adminCustomers),
    [selectedCustomerScopeInput, adminCustomers]
  );

  const selectedCustomerId = selectedCustomerScopeId || inferredCustomerId;

  const matchingScopeCustomers = useMemo(
    () => filterCustomersByNameOrId(adminCustomers, selectedCustomerScopeInput),
    [adminCustomers, selectedCustomerScopeInput]
  );

  useEffect(() => {
    if (isAdmin && selectedCustomerId) {
      setActiveCustomerId(selectedCustomerId);
    }
  }, [isAdmin, selectedCustomerId]);

  const selectedProfileQuery = useQuery({
    queryKey: ["customers", "details", selectedCustomerId],
    queryFn: () => fetchCustomerDetails(selectedCustomerId),
    enabled: isAdmin && Boolean(selectedCustomerId)
  });

  const createMutation = useMutation({
    mutationFn: createCustomerProfile,
    onSuccess: async (profile) => {
      setCreateError(null);
      setFeedback(`Customer created: ${profile.customerId}.`);
      setCreateForm(initialCreate);
      setSelectedCustomerScopeInput(profile.customerId);
      setSelectedCustomerScopeId(profile.customerId);
      await queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
    onError: (error) => {
      setCreateError(`Unable to create customer: ${(error as Error).message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ customerId, payload }: { customerId?: string; payload: UpdateCustomerProfileInput }) =>
      updateCustomerProfile(payload, customerId),
    onSuccess: async (profile) => {
      setUpdateError(null);
      setFeedback(`Customer updated: ${profile.customerId}.`);
      await queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
    onError: (error) => {
      setUpdateError(`Unable to update customer: ${(error as Error).message}`);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (customerId?: string) => deleteCustomerProfile(customerId),
    onSuccess: async (result) => {
      setDeleteError(null);
      setFeedback(`Customer ${result.status.toLowerCase()}: ${result.message}`);
      if (isAdmin) {
        setSelectedCustomerScopeInput("");
        setSelectedCustomerScopeId("");
        clearActiveCustomerId();
      } else {
        clearAuthSession();
        navigate("/login", { replace: true });
      }
      await queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
    onError: (error) => {
      setDeleteError(`Unable to delete customer: ${(error as Error).message}`);
    }
  });

  const onCreateCustomer = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCreateError(null);

    const normalizedPassword = createForm.password?.trim() ?? "";
    const normalizedPhoneNumber = createForm.phoneNumber.trim();
    if (isAdmin && normalizedPassword.length < 8) {
      setCreateError("Password must be at least 8 characters for admin-created customer profiles.");
      return;
    }

    const phoneValidationError = validatePhoneNumber(normalizedPhoneNumber);
    if (phoneValidationError) {
      setCreateError(phoneValidationError);
      return;
    }

    createMutation.mutate({
      externalCustomerKey: createForm.externalCustomerKey.trim(),
      legalName: createForm.legalName.trim(),
      primaryEmail: createForm.primaryEmail.trim(),
      phoneNumber: normalizedPhoneNumber,
      password: isAdmin ? normalizedPassword : undefined
    });
  };

  const onUpdateCustomer = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedPhoneNumber = updateForm.phoneNumber?.trim() ?? "";
    const phoneValidationError = validatePhoneNumber(normalizedPhoneNumber);
    if (phoneValidationError) {
      setUpdateError(phoneValidationError);
      return;
    }

    const payload: UpdateCustomerProfileInput = {
      legalName: updateForm.legalName?.trim() || undefined,
      primaryEmail: updateForm.primaryEmail?.trim() || undefined,
      phoneNumber: normalizedPhoneNumber || undefined,
      status: isAdmin ? updateForm.status : undefined
    };

    const hasChanges = Boolean(
      payload.legalName ||
      payload.primaryEmail ||
      payload.phoneNumber ||
      (isAdmin && payload.status)
    );
    if (!hasChanges) {
      setUpdateError("Provide at least one customer field to update.");
      return;
    }

    if (isAdmin && !selectedCustomerId.trim()) {
      setUpdateError("Enter customer name or ID scope before updating as admin.");
      return;
    }

    setUpdateError(null);

    updateMutation.mutate({
      customerId: isAdmin ? selectedCustomerId.trim() : undefined,
      payload
    });
  };

  const onDeleteCustomer = () => {
    if (isAdmin && !selectedCustomerId.trim()) {
      setDeleteError("Enter customer name or ID scope before deleting as admin.");
      return;
    }

    setDeleteError(null);

    deleteMutation.mutate(isAdmin ? selectedCustomerId.trim() : undefined);
  };

  const onRequestCloseAccount = () => {
    if (deleteMutation.isPending) {
      return;
    }
    setDeleteError(null);
    setIsCloseConfirmOpen(true);
  };

  const onCancelCloseAccount = () => {
    setIsCloseConfirmOpen(false);
  };

  const onConfirmCloseAccount = () => {
    setIsCloseConfirmOpen(false);
    onDeleteCustomer();
  };

  const shownProfile = isAdmin ? selectedProfileQuery.data : selfProfileQuery.data;
  const showCustomerFeedbackAlert = !isAdmin && feedback !== initialFeedbackMessage;
  const updateCustomerProfileCard = (
    <article className="surface-card">
      <h3>Update customer profile</h3>
      <form className="form" onSubmit={onUpdateCustomer}>
        <label>
          Legal name
          <input
            value={updateForm.legalName ?? ""}
            onChange={(event) =>
              setUpdateForm((previous) => ({
                ...previous,
                legalName: event.target.value
              }))
            }
            placeholder="No change"
          />
        </label>

        <label>
          Primary email
          <input
            type="email"
            value={updateForm.primaryEmail ?? ""}
            onChange={(event) =>
              setUpdateForm((previous) => ({
                ...previous,
                primaryEmail: event.target.value
              }))
            }
            placeholder="No change"
          />
        </label>

        <label>
          Phone number
          <input
            value={updateForm.phoneNumber ?? ""}
            onChange={(event) =>
              setUpdateForm((previous) => ({
                ...previous,
                phoneNumber: event.target.value
              }))
            }
            maxLength={PHONE_NUMBER_MAX_LENGTH}
            placeholder="No change"
          />
        </label>

        {isAdmin ? (
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
        ) : null}

        <div className="actions">
          <button type="submit" disabled={updateMutation.isPending}>
            {updateMutation.isPending ? "Updating..." : "Update customer"}
          </button>
          {isAdmin ? (
            <button
              type="button"
              className="button-secondary"
              onClick={onDeleteCustomer}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? "Deleting..." : "Delete customer"}
            </button>
          ) : null}
        </div>
        {updateError ? <p className="inline-error" role="alert">{updateError}</p> : null}
        {isAdmin && deleteError ? <p className="inline-error" role="alert">{deleteError}</p> : null}
      </form>
    </article>
  );

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Customer management</h2>
          <p className="page-subtitle">Retrieve and update customer profiles for this role scope. Admins can create and delete.</p>
        </div>
      </header>

      {isAdmin ? (
        <article className="surface-card">
          <h3>Admin customer scope</h3>
          <form className="form" onSubmit={(event) => event.preventDefault()}>
            <label>
              Target customer name or ID
              <input
                value={selectedCustomerScopeInput}
                onChange={(event) => {
                  setSelectedCustomerScopeInput(event.target.value);
                  setSelectedCustomerScopeId("");
                }}
                placeholder="Search by customer name or ID"
              />
            </label>

            <label>
              Matching customers
              <select
                value={selectedCustomerId}
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
          {selectedCustomerScopeInput.trim() && !selectedCustomerId ? (
            <p className="hint-text">Select a customer from suggestions or provide an exact customer ID.</p>
          ) : null}
        </article>
      ) : null}

      <section className="two-column-grid">
        {isAdmin ? (
          <article className="surface-card">
            <h3>Create customer</h3>
            <form className="form" onSubmit={onCreateCustomer}>
              <label>
                External customer key
                <input
                  value={createForm.externalCustomerKey}
                  onChange={(event) =>
                    setCreateForm((previous) => ({
                      ...previous,
                      externalCustomerKey: event.target.value
                    }))
                  }
                  placeholder="ext-customer-001"
                  required
                />
              </label>

              <label>
                Legal name
                <input
                  value={createForm.legalName}
                  onChange={(event) =>
                    setCreateForm((previous) => ({
                      ...previous,
                      legalName: event.target.value
                    }))
                  }
                  required
                />
              </label>

              <label>
                Primary email
                <input
                  type="email"
                  value={createForm.primaryEmail}
                  onChange={(event) =>
                    setCreateForm((previous) => ({
                      ...previous,
                      primaryEmail: event.target.value
                    }))
                  }
                  required
                />
              </label>

              <label>
                Phone number
                <input
                  value={createForm.phoneNumber}
                  onChange={(event) =>
                    setCreateForm((previous) => ({
                      ...previous,
                      phoneNumber: event.target.value
                    }))
                  }
                  maxLength={PHONE_NUMBER_MAX_LENGTH}
                />
              </label>

              <label>
                Temporary password
                <input
                  type="password"
                  value={createForm.password ?? ""}
                  onChange={(event) =>
                    setCreateForm((previous) => ({
                      ...previous,
                      password: event.target.value
                    }))
                  }
                  placeholder="At least 8 characters"
                  minLength={8}
                  required
                />
              </label>

              <div className="actions">
                <button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? "Creating..." : "Create customer"}
                </button>
              </div>
              {createError ? <p className="inline-error" role="alert">{createError}</p> : null}
            </form>
          </article>
        ) : null}

        <article className="surface-card">
          <h3>Get customer details</h3>
          {!shownProfile ? (
            <p className="hint-text">
              {isAdmin
                ? selectedCustomerId
                  ? selectedProfileQuery.isLoading
                    ? "Loading customer details..."
                    : selectedProfileQuery.isError
                      ? (selectedProfileQuery.error as Error).message
                      : "No customer profile found for this scope."
                  : selectedCustomerScopeInput.trim()
                    ? "Provide an exact customer name or ID to retrieve details."
                    : "Enter a customer name or ID to retrieve details."
                : selfProfileQuery.isLoading
                  ? "Loading your customer profile..."
                  : selfProfileQuery.isError
                    ? (selfProfileQuery.error as Error).message
                    : "No customer profile found for this sign-in."}
            </p>
          ) : (
            <CustomerDetails profile={shownProfile} />
          )}
        </article>

        {!isAdmin ? updateCustomerProfileCard : null}
      </section>

      {isAdmin ? (
        <section className="two-column-grid">
          {updateCustomerProfileCard}

          <article className="surface-card">
            <h3>Operation status</h3>
            <p className="hint-text">{feedback}</p>
          </article>
        </section>
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

      {!isAdmin ? (
        <article className="surface-card customer-close-card">
          <h3>Close your customer account</h3>
          <p className="customer-close-note">
            Closing your account removes customer profile access for this sign-in. This action may be irreversible.
          </p>
          <div className="actions">
            <button
              type="button"
              className="button-danger"
              onClick={onRequestCloseAccount}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? "Closing account..." : "Close my customer account"}
            </button>
          </div>
          {deleteError ? <p className="inline-error" role="alert">{deleteError}</p> : null}
        </article>
      ) : null}

      {!isAdmin && isCloseConfirmOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={onCancelCloseAccount}>
          <div
            className="confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="close-account-confirm-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h3 id="close-account-confirm-title">Confirm account closure</h3>
            <p>Are you sure you want to close your customer account?</p>
            <p>You will be signed out immediately after the account is successfully closed.</p>
            <div className="actions">
              <button type="button" className="button-secondary" onClick={onCancelCloseAccount}>
                Cancel
              </button>
              <button type="button" className="button-danger" onClick={onConfirmCloseAccount}>
                Yes, close my account
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function CustomerDetails({ profile }: { profile: CustomerProfile }) {
  return (
    <dl className="profile-grid">
      <div>
        <dt>Customer ID</dt>
        <dd>{profile.customerId}</dd>
      </div>
      <div>
        <dt>Name</dt>
        <dd>{profile.fullName}</dd>
      </div>
      <div>
        <dt>Email</dt>
        <dd>{profile.email}</dd>
      </div>
      <div>
        <dt>Mobile</dt>
        <dd>{profile.mobile || "-"}</dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd>{profile.status}</dd>
      </div>
      <div>
        <dt>Joined</dt>
        <dd>{formatDate(profile.joinedAt)}</dd>
      </div>
    </dl>
  );
}
