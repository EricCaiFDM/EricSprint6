import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, NavLink, Route, Routes, useNavigate } from "react-router-dom";
import { checkHealth } from "./services/api";
import {
  clearAuthSession,
  getAccessToken,
  getNormalizedTokenRole,
  type UserRole
} from "./services/session";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ResetRequestPage } from "./pages/ResetRequestPage";
import { TokenRefreshPage } from "./pages/TokenRefreshPage";
import { AdminDashboardPage } from "./pages/AdminDashboardPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CustomerManagementPage } from "./pages/CustomerManagementPage";
import { AccountManagementPage } from "./pages/AccountManagementPage";
import { PaymentsPage } from "./pages/PaymentsPage";
import { StandingOrdersPage } from "./pages/StandingOrdersPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { StatementsPage } from "./pages/StatementsPage";
import { SpendingInsightsPage } from "./pages/SpendingInsightsPage";

type AuthState = {
  isAuthenticated: boolean;
  role: UserRole | null;
};

function readAuthState(): AuthState {
  return {
    isAuthenticated: Boolean(getAccessToken()),
    role: getNormalizedTokenRole()
  };
}

function roleHomePath(role: UserRole): string {
  return role === "ADMIN" ? "/admin/dashboard" : "/customer/dashboard";
}

export default function App() {
  const navigate = useNavigate();
  const [authState, setAuthState] = useState<AuthState>(() => readAuthState());
  const { isAuthenticated, role } = authState;

  useEffect(() => {
    const syncAuthState = () => {
      setAuthState(readAuthState());
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

  const defaultRoute = !isAuthenticated
    ? "/security/login"
    : role
      ? roleHomePath(role)
      : "/security/login";

  const withRole = (allowedRoles: UserRole[], element: JSX.Element) => {
    if (!isAuthenticated || !role) {
      return <Navigate to="/security/login" replace />;
    }

    if (allowedRoles.includes(role)) {
      return element;
    }

    return <Navigate to={roleHomePath(role)} replace />;
  };

  const customerLegacyTarget = (segment: string): string => {
    if (isAuthenticated && role === "CUSTOMER") {
      return `/customer/${segment}`;
    }
    return defaultRoute;
  };

  const navLinks = role === "ADMIN"
    ? [
        { to: "/admin/dashboard", label: "Admin Dashboard" }
      ]
    : role === "CUSTOMER"
      ? [
          { to: "/customer/dashboard", label: "Dashboard" },
          { to: "/customer/accounts", label: "Accounts" },
          { to: "/customer/payments", label: "Payments" },
          { to: "/customer/scheduled", label: "Scheduled" },
          { to: "/customer/statements", label: "Statements" },
          { to: "/customer/insights", label: "Insights" },
          { to: "/customer/notifications", label: "Notifications" },
          { to: "/customer/profile", label: "Profile" }
        ]
      : [
          { to: "/security/login", label: "Sign In" },
          { to: "/security/register", label: "Register" }
        ];

  const showAdminSwitch = role !== "ADMIN";

  const onLogout = () => {
    clearAuthSession();
    navigate("/security/login", { replace: true });
  };

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
        <ul className="hero-metrics" aria-label={role === "ADMIN" ? "Admin highlights" : "Customer highlights"}>
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
        {navLinks.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => (isActive ? "main-nav-link active" : "main-nav-link")}
          >
            {link.label}
          </NavLink>
        ))}
        {showAdminSwitch ? (
          <NavLink
            to="/security/login"
            className={({ isActive }) =>
              isActive
                ? "main-nav-link main-nav-link--admin-switch active"
                : "main-nav-link main-nav-link--admin-switch"
            }
          >
            Admin Pages
          </NavLink>
        ) : null}
        {isAuthenticated ? (
          <button
            type="button"
            className="main-nav-action main-nav-action--logout"
            onClick={onLogout}
          >
            Log out
          </button>
        ) : null}
      </nav>

      <section className="content-layout">
        <section className="content-surface">
          <Routes>
            <Route path="/" element={<Navigate to={defaultRoute} replace />} />

            <Route path="/customer" element={<Navigate to="/customer/dashboard" replace />} />
            <Route path="/customer/dashboard" element={withRole(["CUSTOMER"], <DashboardPage />)} />
            <Route path="/customer/accounts" element={withRole(["CUSTOMER"], <AccountManagementPage />)} />
            <Route path="/customer/payments" element={withRole(["CUSTOMER"], <PaymentsPage />)} />
            <Route path="/customer/scheduled" element={withRole(["CUSTOMER"], <StandingOrdersPage />)} />
            <Route path="/customer/notifications" element={withRole(["CUSTOMER"], <NotificationsPage />)} />
            <Route path="/customer/statements" element={withRole(["CUSTOMER"], <StatementsPage />)} />
            <Route path="/customer/insights" element={withRole(["CUSTOMER"], <SpendingInsightsPage />)} />
            <Route path="/customer/profile" element={withRole(["CUSTOMER"], <CustomerManagementPage />)} />

            <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="/admin/dashboard" element={withRole(["ADMIN"], <AdminDashboardPage />)} />

            <Route path="/security/login" element={<LoginPage />} />
            <Route path="/security/register" element={<RegisterPage />} />
            <Route path="/security/create-customer" element={<Navigate to="/security/register" replace />} />
            <Route path="/security/reset" element={<ResetRequestPage />} />
            <Route path="/security/refresh" element={<TokenRefreshPage />} />

            <Route path="/login" element={<Navigate to="/security/login" replace />} />
            <Route path="/register" element={<Navigate to="/security/register" replace />} />
            <Route path="/create-customer" element={<Navigate to="/security/register" replace />} />
            <Route path="/reset" element={<Navigate to="/security/reset" replace />} />
            <Route path="/refresh" element={<Navigate to="/security/refresh" replace />} />

            <Route path="/dashboard" element={<Navigate to={customerLegacyTarget("dashboard")} replace />} />
            <Route path="/accounts" element={<Navigate to={customerLegacyTarget("accounts")} replace />} />
            <Route path="/payments" element={<Navigate to={customerLegacyTarget("payments")} replace />} />
            <Route path="/scheduled" element={<Navigate to={customerLegacyTarget("scheduled")} replace />} />
            <Route path="/notifications" element={<Navigate to={customerLegacyTarget("notifications")} replace />} />
            <Route path="/statements" element={<Navigate to={customerLegacyTarget("statements")} replace />} />
            <Route path="/insights" element={<Navigate to={customerLegacyTarget("insights")} replace />} />
            <Route path="/profile" element={<Navigate to={customerLegacyTarget("profile")} replace />} />

            <Route path="*" element={<Navigate to={defaultRoute} replace />} />
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
            <h2>Accounts & security</h2>
            <ul className="quick-links">
              <li><NavLink to="/security/register">Create account</NavLink></li>
              <li><NavLink to="/security/login">Sign in</NavLink></li>
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
