import { useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import { checkHealth } from "./services/api";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ResetRequestPage } from "./pages/ResetRequestPage";
import { TokenRefreshPage } from "./pages/TokenRefreshPage";

export default function App() {
  const [health, setHealth] = useState("Checking backend...");

  useEffect(() => {
    checkHealth()
      .then((value) => setHealth(`Backend status: ${value}`))
      .catch(() => setHealth("Backend status: unavailable"));
  }, []);

  return (
    <main className="layout">
      <header className="hero">
        <h1>Banking Frontend</h1>
        <p>{health}</p>
      </header>
      <section className="panel">
        <nav className="tabs" aria-label="Authentication pages">
          <NavLink to="/register" className={({ isActive }) => isActive ? "tab active" : "tab"}>Register</NavLink>
          <NavLink to="/login" className={({ isActive }) => isActive ? "tab active" : "tab"}>Login</NavLink>
          <NavLink to="/reset" className={({ isActive }) => isActive ? "tab active" : "tab"}>Reset Request</NavLink>
          <NavLink to="/refresh" className={({ isActive }) => isActive ? "tab active" : "tab"}>Token Refresh</NavLink>
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
    </main>
  );
}
