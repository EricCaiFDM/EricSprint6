import { useEffect, useState } from "react";
import { checkHealth } from "./services/api";
import { AuthPage } from "./pages/AuthPage";

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
        <AuthPage />
      </section>
    </main>
  );
}
