import { getTokenEmail, getTokenSubject } from "../services/session";

export function AdminDashboardPage() {
  const signedInEmail = getTokenEmail() ?? "Not available";
  const signedInUserId = getTokenSubject() ?? "Not available";

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Admin workspace</h2>
          <p className="page-subtitle">
            Dedicated area for administrative workflows and cross-customer oversight.
          </p>
        </div>
      </header>

      <article className="surface-card">
        <h3>Current admin session</h3>
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

      <article className="surface-card">
        <h3>Admin route separation</h3>
        <p className="hint-text">
          Customer banking pages are intentionally separated from this admin section. Use this
          workspace for administrative operations, while customer users remain in the customer area.
        </p>
      </article>
    </section>
  );
}
