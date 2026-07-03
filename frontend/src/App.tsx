import { useCallback, useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, NavLink, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { checkHealth, refreshToken as refreshSessionToken } from "./services/api";
import { fetchRecentNotifications } from "./services/notifications";
import {
  clearAuthSession,
  getAccessToken,
  getAccessTokenExpiresAtMs,
  getNormalizedTokenRole,
  getRefreshToken,
  type UserRole
} from "./services/session";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ResetRequestPage } from "./pages/ResetRequestPage";
import { AdminDashboardPage } from "./pages/AdminDashboardPage";
import { AdminCustomerDetailsPage } from "./pages/AdminCustomerDetailsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CustomerManagementPage } from "./pages/CustomerManagementPage";
import { AccountManagementPage } from "./pages/AccountManagementPage";
import { AccountDetailsPage } from "./pages/AccountDetailsPage";
import { PaymentsPage } from "./pages/PaymentsPage";
import { StandingOrdersPage } from "./pages/StandingOrdersPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { StatementsPage } from "./pages/StatementsPage";
import { StatementDetailsPage } from "./pages/StatementDetailsPage";
import { SpendingInsightsPage } from "./pages/SpendingInsightsPage";

type AuthState = {
  isAuthenticated: boolean;
  accessToken: string | null;
  role: UserRole | null;
};

const SESSION_WARNING_WINDOW_MS = 3 * 60 * 1000;

function readAuthState(): AuthState {
  const accessToken = getAccessToken();
  return {
    isAuthenticated: Boolean(accessToken),
    accessToken,
    role: getNormalizedTokenRole()
  };
}

function roleHomePath(role: UserRole): string {
  return role === "ADMIN" ? "/admin/dashboard" : "/customer/dashboard";
}

function roleWorkspacePrefix(role: UserRole): "admin" | "customer" {
  return role === "ADMIN" ? "admin" : "customer";
}

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const [authState, setAuthState] = useState<AuthState>(() => readAuthState());
  const [screenAlert, setScreenAlert] = useState<string | null>(null);
  const [isNavMenuOpen, setIsNavMenuOpen] = useState(false);
  const [showSessionExpiryDialog, setShowSessionExpiryDialog] = useState(false);
  const [isRefreshingSession, setIsRefreshingSession] = useState(false);
  const [sessionRefreshError, setSessionRefreshError] = useState<string | null>(null);
  const latestNotificationIdRef = useRef<string | null>(null);
  const sessionWarningTimeoutRef = useRef<number | null>(null);
  const sessionExpiryTimeoutRef = useRef<number | null>(null);
  const { isAuthenticated, role, accessToken } = authState;
  const shouldTrackCustomerFeed = isAuthenticated && role === "CUSTOMER";

  const clearSessionExpiryTimeouts = useCallback(() => {
    if (sessionWarningTimeoutRef.current !== null) {
      window.clearTimeout(sessionWarningTimeoutRef.current);
      sessionWarningTimeoutRef.current = null;
    }
    if (sessionExpiryTimeoutRef.current !== null) {
      window.clearTimeout(sessionExpiryTimeoutRef.current);
      sessionExpiryTimeoutRef.current = null;
    }
  }, []);

  const onLogout = useCallback(() => {
    clearSessionExpiryTimeouts();
    setShowSessionExpiryDialog(false);
    setIsRefreshingSession(false);
    setSessionRefreshError(null);
    clearAuthSession();
    navigate("/security/login", { replace: true });
  }, [clearSessionExpiryTimeouts, navigate]);

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

  const notificationFeedQuery = useQuery({
    queryKey: ["notification-feed"],
    queryFn: fetchRecentNotifications,
    enabled: shouldTrackCustomerFeed,
    refetchInterval: 5000,
    refetchIntervalInBackground: true
  });

  useEffect(() => {
    if (shouldTrackCustomerFeed) {
      return;
    }
    latestNotificationIdRef.current = null;
    setScreenAlert(null);
  }, [shouldTrackCustomerFeed]);

  useEffect(() => {
    const latest = notificationFeedQuery.data?.[0];
    if (!latest) {
      return;
    }

    if (latestNotificationIdRef.current === null) {
      latestNotificationIdRef.current = latest.notificationId;
      return;
    }

    if (latest.notificationId === latestNotificationIdRef.current) {
      return;
    }

    latestNotificationIdRef.current = latest.notificationId;

    if (location.pathname !== "/customer/notifications" && location.pathname !== "/customer/payments") {
      setScreenAlert(`New alert: ${latest.title}`);
    }
  }, [location.pathname, notificationFeedQuery.data]);

  useEffect(() => {
    if ((location.pathname === "/customer/notifications" || location.pathname === "/customer/payments") && screenAlert) {
      setScreenAlert(null);
    }
  }, [location.pathname, screenAlert]);

  useEffect(() => {
    if (!screenAlert) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setScreenAlert(null);
    }, 4500);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [screenAlert]);

  useEffect(() => {
    setIsNavMenuOpen(false);
  }, [location.pathname, isAuthenticated, role]);

  useEffect(() => {
    clearSessionExpiryTimeouts();
    setShowSessionExpiryDialog(false);
    setIsRefreshingSession(false);
    setSessionRefreshError(null);

    if (!isAuthenticated) {
      return;
    }

    const expiresAtMs = getAccessTokenExpiresAtMs();
    if (!expiresAtMs) {
      return;
    }

    const now = Date.now();
    const msUntilExpiry = expiresAtMs - now;

    if (msUntilExpiry <= 0) {
      onLogout();
      return;
    }

    const msUntilWarning = Math.max(msUntilExpiry - SESSION_WARNING_WINDOW_MS, 0);

    sessionWarningTimeoutRef.current = window.setTimeout(() => {
      setSessionRefreshError(null);
      setShowSessionExpiryDialog(true);
    }, msUntilWarning);

    sessionExpiryTimeoutRef.current = window.setTimeout(() => {
      onLogout();
    }, msUntilExpiry);

    return clearSessionExpiryTimeouts;
  }, [accessToken, clearSessionExpiryTimeouts, isAuthenticated, onLogout]);

  const onStaySignedIn = async () => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      onLogout();
      return;
    }

    setIsRefreshingSession(true);
    setSessionRefreshError(null);

    try {
      await refreshSessionToken({ refreshToken });
      setShowSessionExpiryDialog(false);
    } catch {
      setSessionRefreshError("Unable to extend session. Please try again or log out.");
    } finally {
      setIsRefreshingSession(false);
    }
  };

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

  const roleScopedTarget = (segment: string): string => {
    if (isAuthenticated && role) {
      return `/${roleWorkspacePrefix(role)}/${segment}`;
    }
    return defaultRoute;
  };

  const navLinks = role === "ADMIN"
    ? [
        { to: "/admin/dashboard", label: "Admin Dashboard" },
        { to: "/admin/accounts", label: "Accounts" },
        { to: "/admin/payments", label: "Payments" },
        { to: "/admin/statements", label: "Statements" },
        { to: "/admin/profile", label: "Customers" }
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

  const showAdminSwitch = !isAuthenticated;

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
      <nav className={isNavMenuOpen ? "main-nav main-nav--open" : "main-nav"} aria-label="Primary banking navigation">
        <button
          type="button"
          className="main-nav-toggle"
          aria-expanded={isNavMenuOpen}
          aria-controls="primary-banking-nav-links"
          onClick={() => setIsNavMenuOpen((isOpen) => !isOpen)}
        >
          <span className="main-nav-toggle-icon" aria-hidden="true">
            <span />
            <span />
            <span />
          </span>
          <span>{isNavMenuOpen ? "Close menu" : "Menu"}</span>
        </button>

        <div id="primary-banking-nav-links" className="main-nav-links">
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => (isActive ? "main-nav-link active" : "main-nav-link")}
              onClick={() => setIsNavMenuOpen(false)}
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
              onClick={() => setIsNavMenuOpen(false)}
            >
              Admin Pages
            </NavLink>
          ) : null}
          {isAuthenticated ? (
            <button
              type="button"
              className="main-nav-action main-nav-action--logout"
              onClick={() => {
                setIsNavMenuOpen(false);
                onLogout();
              }}
            >
              Log out
            </button>
          ) : null}
        </div>
      </nav>

      {screenAlert ? (
        <aside className="screen-snackbar" role="alert" aria-live="assertive" aria-atomic="true">
          <p className="screen-snackbar-message">{screenAlert}</p>
          <button type="button" className="screen-snackbar-dismiss" onClick={() => setScreenAlert(null)}>
            Dismiss
          </button>
        </aside>
      ) : null}

      {showSessionExpiryDialog && isAuthenticated ? (
        <div className="modal-backdrop" role="presentation">
          <section
            className="confirm-modal session-expiry-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="session-expiry-title"
            aria-describedby="session-expiry-description"
          >
            <h3 id="session-expiry-title">Session expiring soon</h3>
            <p id="session-expiry-description">Your session is about to expire. Stay signed in?</p>
            {sessionRefreshError ? <p className="inline-error">{sessionRefreshError}</p> : null}
            <div className="actions">
              <button
                type="button"
                className="button-secondary"
                onClick={onLogout}
                disabled={isRefreshingSession}
              >
                Log out
              </button>
              <button type="button" onClick={onStaySignedIn} disabled={isRefreshingSession}>
                {isRefreshingSession ? "Refreshing..." : "Stay signed in"}
              </button>
            </div>
          </section>
        </div>
      ) : null}

      <section className="content-layout">
        <section className="content-surface">
          <Routes>
            <Route path="/" element={<Navigate to={defaultRoute} replace />} />

            <Route path="/customer" element={<Navigate to="/customer/dashboard" replace />} />
            <Route path="/customer/dashboard" element={withRole(["CUSTOMER"], <DashboardPage />)} />
            <Route path="/customer/accounts" element={withRole(["CUSTOMER"], <AccountManagementPage />)} />
            <Route path="/customer/accounts/:accountId" element={withRole(["CUSTOMER"], <AccountDetailsPage />)} />
            <Route path="/customer/payments" element={withRole(["CUSTOMER"], <PaymentsPage />)} />
            <Route path="/customer/scheduled" element={withRole(["CUSTOMER"], <StandingOrdersPage />)} />
            <Route path="/customer/notifications" element={withRole(["CUSTOMER"], <NotificationsPage />)} />
            <Route path="/customer/statements" element={withRole(["CUSTOMER"], <StatementsPage />)} />
            <Route path="/customer/statements/:statementId" element={withRole(["CUSTOMER"], <StatementDetailsPage />)} />
            <Route path="/customer/insights" element={withRole(["CUSTOMER"], <SpendingInsightsPage />)} />
            <Route path="/customer/profile" element={withRole(["CUSTOMER"], <CustomerManagementPage />)} />

            <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="/admin/dashboard" element={withRole(["ADMIN"], <AdminDashboardPage />)} />
            <Route path="/admin/customers/:customerId" element={withRole(["ADMIN"], <AdminCustomerDetailsPage />)} />
            <Route path="/admin/accounts" element={withRole(["ADMIN"], <AccountManagementPage />)} />
            <Route path="/admin/accounts/:accountId" element={withRole(["ADMIN"], <AccountDetailsPage />)} />
            <Route path="/admin/payments" element={withRole(["ADMIN"], <PaymentsPage />)} />
            <Route path="/admin/statements" element={withRole(["ADMIN"], <StatementsPage />)} />
            <Route path="/admin/statements/:statementId" element={withRole(["ADMIN"], <StatementDetailsPage />)} />
            <Route path="/admin/profile" element={withRole(["ADMIN"], <CustomerManagementPage />)} />

            <Route path="/security/login" element={<LoginPage />} />
            <Route path="/security/register" element={<RegisterPage />} />
            <Route path="/security/create-customer" element={<Navigate to="/security/register" replace />} />
            <Route path="/security/reset" element={<ResetRequestPage />} />
            <Route path="/security/refresh" element={<Navigate to="/security/login" replace />} />

            <Route path="/login" element={<Navigate to="/security/login" replace />} />
            <Route path="/register" element={<Navigate to="/security/register" replace />} />
            <Route path="/create-customer" element={<Navigate to="/security/register" replace />} />
            <Route path="/reset" element={<Navigate to="/security/reset" replace />} />
            <Route path="/refresh" element={<Navigate to="/security/login" replace />} />

            <Route path="/dashboard" element={<Navigate to={roleScopedTarget("dashboard")} replace />} />
            <Route path="/accounts" element={<Navigate to={roleScopedTarget("accounts")} replace />} />
            <Route path="/payments" element={<Navigate to={roleScopedTarget("payments")} replace />} />
            <Route path="/scheduled" element={<Navigate to={roleScopedTarget("scheduled")} replace />} />
            <Route path="/notifications" element={<Navigate to={roleScopedTarget("notifications")} replace />} />
            <Route path="/statements" element={<Navigate to={roleScopedTarget("statements")} replace />} />
            <Route path="/insights" element={<Navigate to={roleScopedTarget("insights")} replace />} />
            <Route path="/profile" element={<Navigate to={roleScopedTarget("profile")} replace />} />

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
