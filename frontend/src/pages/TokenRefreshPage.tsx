import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { refreshToken } from "../services/api";

export function TokenRefreshPage() {
  const [token, setToken] = useState("");
  const [output, setOutput] = useState("No request yet.");
  const refreshMutation = useMutation({
    mutationFn: refreshToken
  });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await refreshMutation.mutateAsync({ refreshToken: token });
      setOutput(`Refresh success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Refresh failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Refresh secure session</h2>
      <p>Exchange a valid refresh token for a new access token without repeating full authentication.</p>
      <form onSubmit={onSubmit} className="form">
        <label>
          Refresh Token
          <input
            value={token}
            onChange={(e) => setToken(e.target.value)}
            type="text"
            placeholder="Paste refresh token"
            required
          />
        </label>
        <div className="actions">
          <button type="submit" disabled={refreshMutation.isPending}>
            {refreshMutation.isPending ? "Refreshing..." : "Refresh Session"}
          </button>
        </div>
      </form>
      <pre className="output">{output}</pre>
    </article>
  );
}
