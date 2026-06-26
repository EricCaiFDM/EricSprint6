import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import { checkHealth } from "./services/api";
import { getAccessToken } from "./services/session";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { CreateCustomerPage } from "./pages/CreateCustomerPage";
import { ResetRequestPage } from "./pages/ResetRequestPage";
import { TokenRefreshPage } from "./pages/TokenRefreshPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CustomerManagementPage } from "./pages/CustomerManagementPage";
import { AccountManagementPage } from "./pages/AccountManagementPage";
import { PaymentsPage } from "./pages/PaymentsPage";
import { StandingOrdersPage } from "./pages/StandingOrdersPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { StatementsPage } from "./pages/StatementsPage";
import { SpendingInsightsPage } from "./pages/SpendingInsightsPage";

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAccessToken()));

  useEffect(() => {
    const syncAuthState = () => {
      setIsAuthenticated(Boolean(getAccessToken()));
    };

    syncAuthState();
    window.addEventListener("storage", syncAuthState);
    window.addEventListener("nb-auth-changed", syncAuthState as EventListener);

    return () => {
      window.removeEventListener("storage", syncAuthState);
      window.removeEventListener("nb-auth-changed", syncAuthState as EventListener);
    };
  }, []);

  const healthQuery = useQuery({
    queryKey: ["health"],
    queryFn: checkHealth,
    retry: 0
  });

  const healthState: "checking" | "online" | "offline" = healthQuery.isPending
    ? "checking"
    : healthQuery.isError
      ? "offline"
      : "online";

  const health = healthQuery.isPending
    ? "Checking service status..."
    : healthQuery.isError
      ? "Some banking services are unavailable"
      : "All banking services are available";

  const withAuth = (element: JSX.Element) =>
    isAuthenticated ? element : <Navigate to="/security/login" replace />;

  return (
    <main className="bank-app">
      <header className="top-header">
        <div className="brand-row">
          <div className="brand-mark" aria-hidden="true">NB</div>
          <div>
            <p className="eyebrow">NorthBridge Personal Banking</p>
            <h1>Everyday banking, designed for clarity</h1>
            <p className={`service-pill service-pill--${healthState}`}>{health}</p>
          </div>
        </div>
        <ul className="hero-metrics" aria-label="Customer highlights">
          <li>
            <strong>Realtime</strong>
            <span>Balance Updates</span>
          </li>
          <li>
            <strong>Smart</strong>
            <span>Spending Insights</span>
          </li>
          <li>
            <strong>Secure</strong>
            <span>Protected Sessions</span>
          </li>
        </ul>
      </header>
      <nav className="main-nav" aria-label="Primary banking navigation">
        <NavLink to="/dashboard" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Dashboard</NavLink>
        <NavLink to="/accounts" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Accounts</NavLink>
        <NavLink to="/payments" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Payments</NavLink>
        <NavLink to="/scheduled" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Scheduled</NavLink>
        <NavLink to="/statements" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Statements</NavLink>
        <NavLink to="/insights" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Insights</NavLink>
        <NavLink to="/notifications" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Notifications</NavLink>
        <NavLink to="/profile" className={({ isActive }) => isActive ? "main-nav-link active" : "main-nav-link"}>Profile</NavLink>
      </nav>

      <section className="content-layout">
        <section className="content-surface">
          <Routes>
            <Route path="/" element={<Navigate to={isAuthenticated ? "/dashboard" : "/security/login"} replace />} />
            <Route path="/dashboard" element={withAuth(<DashboardPage />)} />
            <Route path="/accounts" element={withAuth(<AccountManagementPage />)} />
            <Route path="/payments" element={withAuth(<PaymentsPage />)} />
            <Route path="/scheduled" element={withAuth(<StandingOrdersPage />)} />
            <Route path="/notifications" element={withAuth(<NotificationsPage />)} />
            <Route path="/statements" element={withAuth(<StatementsPage />)} />
            <Route path="/insights" element={withAuth(<SpendingInsightsPage />)} />
            <Route path="/profile" element={withAuth(<CustomerManagementPage />)} />

            <Route path="/security/login" element={<LoginPage />} />
            <Route path="/security/register" element={<RegisterPage />} />
            <Route path="/security/create-customer" element={<CreateCustomerPage />} />
            <Route path="/security/reset" element={<ResetRequestPage />} />
            <Route path="/security/refresh" element={<TokenRefreshPage />} />

            <Route path="/login" element={<Navigate to="/security/login" replace />} />
            <Route path="/register" element={<Navigate to="/security/register" replace />} />
            <Route path="/create-customer" element={<Navigate to="/security/create-customer" replace />} />
            <Route path="/reset" element={<Navigate to="/security/reset" replace />} />
            <Route path="/refresh" element={<Navigate to="/security/refresh" replace />} />

            <Route path="*" element={<Navigate to={isAuthenticated ? "/dashboard" : "/security/login"} replace />} />
          </Routes>
        </section>

        <aside className="side-rail" aria-label="Customer support and security">
          <article className="rail-card">
            <h2>Need help?</h2>
            <p>Our support team is available every day from 7:00 AM to 10:00 PM.</p>
            <ul className="quick-links">
              <li><a href="tel:+61000000000">Call support</a></li>
              <li><a href="mailto:support@northbridgebank.com">Message support</a></li>
            </ul>
          </article>

          <article className="rail-card">
            <h2>Onboarding & security</h2>
            <ul className="quick-links">
              <li><NavLink to="/security/register">Create access profile</NavLink></li>
              <li><NavLink to="/security/login">Sign in</NavLink></li>
              <li><NavLink to="/security/create-customer">Create customer profile</NavLink></li>
              <li><NavLink to="/security/reset">Recover account</NavLink></li>
              <li><NavLink to="/security/refresh">Refresh session</NavLink></li>
            </ul>
          </article>

          <article className="rail-card">
            <h2>Trust at NorthBridge</h2>
          <ul>
            <li>Multi-factor security and fraud monitoring</li>
            <li>Instant push alerts for key account activity</li>
            <li>Dedicated support with transparent case tracking</li>
          </ul>
          <p className="trust-note">Demo environment: please do not enter real personal data.</p>
          </article>
        </aside>
      </section>
    </main>
  );
}
