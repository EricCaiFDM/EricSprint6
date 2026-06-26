import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createCustomerProfile,
  deleteCustomerProfile,
  fetchCustomerDetails,
  fetchCustomerProfile,
  updateCustomerProfile,
  type CreateCustomerProfileInput,
  type CustomerProfile,
  type UpdateCustomerProfileInput
} from "../services/customers";
import { clearActiveCustomerId, getNormalizedTokenRole, setActiveCustomerId } from "../services/session";
import { formatDate } from "../utils/formatting";

const initialCreate: CreateCustomerProfileInput = {
  externalCustomerKey: "",
  legalName: "",
  primaryEmail: "",
  phoneNumber: ""
};

const initialUpdate: UpdateCustomerProfileInput = {
  legalName: "",
  primaryEmail: "",
  phoneNumber: "",
  status: undefined
};

export function CustomerManagementPage() {
  const queryClient = useQueryClient();
  const role = getNormalizedTokenRole();
  const isAdmin = role === "ADMIN";

  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const [feedback, setFeedback] = useState("Create, retrieve, and update customer profiles. Admins can delete customers.");
  const [createForm, setCreateForm] = useState<CreateCustomerProfileInput>(initialCreate);
  const [updateForm, setUpdateForm] = useState<UpdateCustomerProfileInput>(initialUpdate);

  const selfProfileQuery = useQuery({
    queryKey: ["customers", "self"],
    queryFn: fetchCustomerProfile,
    enabled: !isAdmin
  });

  const selectedProfileQuery = useQuery({
    queryKey: ["customers", "details", selectedCustomerId],
    queryFn: () => fetchCustomerDetails(selectedCustomerId),
    enabled: isAdmin && Boolean(selectedCustomerId)
  });

  const createMutation = useMutation({
    mutationFn: createCustomerProfile,
    onSuccess: async (profile) => {
      setFeedback(`Customer created: ${profile.customerId}.`);
      setCreateForm(initialCreate);
      setSelectedCustomerId(profile.customerId);
      await queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
    onError: (error) => {
      setFeedback(`Unable to create customer: ${(error as Error).message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ customerId, payload }: { customerId?: string; payload: UpdateCustomerProfileInput }) =>
      updateCustomerProfile(payload, customerId),
    onSuccess: async (profile) => {
      setFeedback(`Customer updated: ${profile.customerId}.`);
      await queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
    onError: (error) => {
      setFeedback(`Unable to update customer: ${(error as Error).message}`);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (customerId?: string) => deleteCustomerProfile(customerId),
    onSuccess: async (result) => {
      setFeedback(`Customer ${result.status.toLowerCase()}: ${result.message}`);
      if (isAdmin) {
        setSelectedCustomerId("");
        clearActiveCustomerId();
      }
      await queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
    onError: (error) => {
      setFeedback(`Unable to delete customer: ${(error as Error).message}`);
    }
  });

  const onCreateCustomer = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    createMutation.mutate({
      externalCustomerKey: createForm.externalCustomerKey.trim(),
      legalName: createForm.legalName.trim(),
      primaryEmail: createForm.primaryEmail.trim(),
      phoneNumber: createForm.phoneNumber.trim()
    });
  };

  const onUpdateCustomer = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const payload: UpdateCustomerProfileInput = {
      legalName: updateForm.legalName?.trim() || undefined,
      primaryEmail: updateForm.primaryEmail?.trim() || undefined,
      phoneNumber: updateForm.phoneNumber?.trim() || undefined,
      status: updateForm.status
    };

    const hasChanges = Boolean(payload.legalName || payload.primaryEmail || payload.phoneNumber || payload.status);
    if (!hasChanges) {
      setFeedback("Provide at least one customer field to update.");
      return;
    }

    if (isAdmin && !selectedCustomerId.trim()) {
      setFeedback("Enter customer ID scope before updating as admin.");
      return;
    }

    updateMutation.mutate({
      customerId: isAdmin ? selectedCustomerId.trim() : undefined,
      payload
    });
  };

  const onDeleteCustomer = () => {
    if (!isAdmin) {
      setFeedback("Only admin accounts can delete customer profiles.");
      return;
    }

    if (isAdmin && !selectedCustomerId.trim()) {
      setFeedback("Enter customer ID scope before deleting as admin.");
      return;
    }

    deleteMutation.mutate(isAdmin ? selectedCustomerId.trim() : undefined);
  };

  const shownProfile = isAdmin ? selectedProfileQuery.data : selfProfileQuery.data;

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Customer management</h2>
          <p className="page-subtitle">Create, retrieve, and update customer profiles for this role scope. Admins can delete.</p>
        </div>
      </header>

      {isAdmin ? (
        <article className="surface-card">
          <h3>Admin customer scope</h3>
          <form className="form" onSubmit={(event) => event.preventDefault()}>
            <label>
              Target customer ID
              <input
                value={selectedCustomerId}
                onChange={(event) => {
                  const value = event.target.value;
                  setSelectedCustomerId(value);
                  if (value.trim()) {
                    setActiveCustomerId(value.trim());
                  }
                }}
                placeholder="customer UUID"
              />
            </label>
          </form>
        </article>
      ) : null}

      <section className="two-column-grid">
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
              />
            </label>

            <div className="actions">
              <button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Creating..." : "Create customer"}
              </button>
            </div>
          </form>
        </article>

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
                  : "Enter a customer ID to retrieve details."
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
      </section>

      <section className="two-column-grid">
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
                placeholder="No change"
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
          </form>
        </article>

        <article className="surface-card">
          <h3>Operation status</h3>
          <p className="hint-text">{feedback}</p>
        </article>
      </section>
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
