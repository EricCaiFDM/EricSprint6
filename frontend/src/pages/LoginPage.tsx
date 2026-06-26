import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { login } from "../services/api";

export function LoginPage() {
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
    } catch (error) {
      setOutput(`Sign in failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Sign in to your account</h2>
      <p>Access your profile, accounts, payments, statements, and personalised insights securely.</p>
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
      <pre className="output">{output}</pre>
    </article>
  );
}
