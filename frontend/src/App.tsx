import { useQuery } from "@tanstack/react-query";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import { checkHealth } from "./services/api";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ResetRequestPage } from "./pages/ResetRequestPage";
import { TokenRefreshPage } from "./pages/TokenRefreshPage";

export default function App() {
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
    ? "Checking secure gateway..."
    : healthQuery.isError
      ? "Core banking services: unavailable"
      : `Core banking services: ${healthQuery.data}`;

  return (
    <main className="layout">
      <header className="hero">
        <div className="hero-main">
          <div className="brand-mark" aria-hidden="true">NB</div>
          <div>
            <p className="eyebrow">NorthBridge Digital Banking</p>
            <h1>Secure Customer Access Portal</h1>
            <p className={`health-pill health-pill--${healthState}`}>{health}</p>
          </div>
        </div>
        <ul className="hero-metrics" aria-label="Service highlights">
          <li>
            <strong>24/7</strong>
            <span>Protected Access</span>
          </li>
          <li>
            <strong>256-bit</strong>
            <span>Transport Security</span>
          </li>
          <li>
            <strong>RBAC</strong>
            <span>Policy Enforcement</span>
          </li>
        </ul>
      </header>
      <section className="workspace">
        <section className="panel">
          <nav className="tabs" aria-label="Authentication pages">
            <NavLink to="/register" className={({ isActive }) => isActive ? "tab active" : "tab"}>Open Account</NavLink>
            <NavLink to="/login" className={({ isActive }) => isActive ? "tab active" : "tab"}>Sign In</NavLink>
            <NavLink to="/reset" className={({ isActive }) => isActive ? "tab active" : "tab"}>Reset Access</NavLink>
            <NavLink to="/refresh" className={({ isActive }) => isActive ? "tab active" : "tab"}>Refresh Session</NavLink>
          </nav>
          <Routes>
            <Route path="/" element={<Navigate to="/register" replace />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/reset" element={<ResetRequestPage />} />
            <Route path="/refresh" element={<TokenRefreshPage />} />
            <Route path="*" element={<Navigate to="/register" replace />} />
          </Routes>
        </section>
        <aside className="trust-panel" aria-label="Security and trust information">
          <h2>Built for modern banking expectations</h2>
          <p>
            Every request path is designed for strong authentication controls, policy-based access, and
            auditable outcomes.
          </p>
          <ul>
            <li>Role and ownership controls on customer resources</li>
            <li>Immutable audit trails for authentication events</li>
            <li>Consistent API contracts for frontend and backend parity</li>
          </ul>
          <p className="trust-note">
            Demo environment only. Avoid submitting real credentials or personally sensitive information.
          </p>
        </aside>
      </section>
    </main>
  );
}
