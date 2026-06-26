import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../services/api";

export function LoginPage() {
  const navigate = useNavigate();
  const [identity, setIdentity] = useState("");
  const [password, setPassword] = useState("");
  const [output, setOutput] = useState("Sign in with your registered email and password.");
  const loginMutation = useMutation({
    mutationFn: login
  });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await loginMutation.mutateAsync({ identity, password });
      const minutes = Math.round(data.expiresIn / 60);
      setOutput(`Welcome back. Your secure session is active for approximately ${minutes} minutes.`);
      navigate("/dashboard", { replace: true });
    } catch (error) {
      setOutput(`Sign in failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Sign in to your account</h2>
      <p>
        Dedicated sign-in page. Use your registered credentials to unlock customer pages and account actions.
      </p>
      <form onSubmit={onSubmit} className="form">
        <label>
          Email address
          <input
            value={identity}
            onChange={(e) => setIdentity(e.target.value)}
            type="email"
            placeholder="you@northbridgebank.com"
            required
          />
        </label>
        <label>
          Password
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            placeholder="Enter your password"
            required
          />
        </label>
        <div className="actions">
          <button type="submit" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? "Signing In..." : "Sign In Securely"}
          </button>
        </div>
      </form>
      <div className="auth-links">
        <strong>Need to complete onboarding?</strong>
        <Link to="/security/register">Create access profile</Link>
        <Link to="/security/create-customer">Create customer profile</Link>
      </div>
      <pre className="output">{output}</pre>
    </article>
  );
}
