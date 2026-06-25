import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { login } from "../services/api";

export function LoginPage() {
  const [identity, setIdentity] = useState("");
  const [password, setPassword] = useState("");
  const [output, setOutput] = useState("No request yet.");
  const loginMutation = useMutation({
    mutationFn: login
  });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await loginMutation.mutateAsync({ identity, password });
      setOutput(`Login success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Login failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Sign in to your account</h2>
      <p>Access customer profiles, account activity, and secured operations through the banking gateway.</p>
      <form onSubmit={onSubmit} className="form">
        <label>
          Work email
          <input
            value={identity}
            onChange={(e) => setIdentity(e.target.value)}
            type="email"
            placeholder="analyst@northbridgebank.com"
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
