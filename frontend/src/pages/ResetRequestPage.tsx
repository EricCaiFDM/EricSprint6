import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { requestPasswordReset } from "../services/api";

export function ResetRequestPage() {
  const [identity, setIdentity] = useState("");
  const [output, setOutput] = useState("No request yet.");
  const resetMutation = useMutation({
    mutationFn: requestPasswordReset
  });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await resetMutation.mutateAsync({ identity });
      setOutput(`Reset request accepted: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Reset request failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Reset account access</h2>
      <p>Submit your registered identity to trigger the controlled password reset workflow.</p>
      <form onSubmit={onSubmit} className="form">
        <label>
          Registered email
          <input
            value={identity}
            onChange={(e) => setIdentity(e.target.value)}
            type="email"
            placeholder="you@northbridgebank.com"
            required
          />
        </label>
        <div className="actions">
          <button type="submit" disabled={resetMutation.isPending}>
            {resetMutation.isPending ? "Submitting..." : "Request Reset"}
          </button>
        </div>
      </form>
      <pre className="output">{output}</pre>
    </article>
  );
}
