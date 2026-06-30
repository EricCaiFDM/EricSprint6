import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { fetchAccounts } from "../services/accounts";
import { fetchCustomersForAdmin } from "../services/customers";
import { getTokenEmail, getTokenSubject, setActiveCustomerId } from "../services/session";
import { filterCustomersByNameOrId } from "../utils/customerScope";
import { formatCurrency, formatDate } from "../utils/formatting";

export function AdminDashboardPage() {
  const navigate = useNavigate();
  const signedInEmail = getTokenEmail() ?? "Not available";
  const signedInUserId = getTokenSubject() ?? "Not available";

  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const [customerSearchQuery, setCustomerSearchQuery] = useState("");
  const [feedback, setFeedback] = useState(
    "Search customers by name or ID, then open a full profile overview."
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

  const selectedCustomer = useMemo(
    () => customers.find((customer) => customer.customerId === selectedCustomerId) ?? null,
    [customers, selectedCustomerId]
  );

  const filteredCustomers = useMemo(
    () => filterCustomersByNameOrId(customers, customerSearchQuery),
    [customers, customerSearchQuery]
  );

  const selectedCustomerBalance = useMemo(
    () => accounts.reduce((sum, account) => sum + account.availableBalance, 0),
    [accounts]
  );

  const onSelectCustomer = (customerId: string) => {
    setSelectedCustomerId(customerId);
    setActiveCustomerId(customerId);
    setFeedback(`Opening customer profile overview for ${customerId}.`);
    navigate(`/admin/customers/${encodeURIComponent(customerId)}`);
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Admin workspace</h2>
          <p className="page-subtitle">
            View all customers and open a full profile page with accounts, transactions, monthly statements, and spending insights.
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
          <form className="form" onSubmit={(event) => event.preventDefault()}>
            <label>
              Search customers by name or ID
              <input
                value={customerSearchQuery}
                onChange={(event) => setCustomerSearchQuery(event.target.value)}
                placeholder="Type a customer name or ID"
              />
            </label>
          </form>
          {customersQuery.isLoading ? (
            <p className="hint-text">Loading customer directory...</p>
          ) : customersQuery.isError ? (
            <p className="hint-text">Unable to load customers: {(customersQuery.error as Error).message}</p>
          ) : customers.length === 0 ? (
            <p className="hint-text">No customers found.</p>
          ) : filteredCustomers.length === 0 ? (
            <p className="hint-text">No customers match your search.</p>
          ) : (
            <ul className="stack-list">
              {filteredCustomers.map((customer) => (
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

      <article className="surface-card">
        <h3>Operation status</h3>
        <p className="hint-text">{feedback}</p>
      </article>
    </section>
  );
}
