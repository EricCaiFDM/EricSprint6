import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { confirmPasswordReset, requestPasswordReset } from "../services/api";
import { Link } from "react-router-dom";

export function ResetRequestPage() {
  const [identity, setIdentity] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [canConfirmReset, setCanConfirmReset] = useState(false);
  const [output, setOutput] = useState("If your email is registered, we will send recovery guidance.");
  const resetRequestMutation = useMutation({
    mutationFn: requestPasswordReset
  });
  const resetConfirmMutation = useMutation({
    mutationFn: confirmPasswordReset
  });

  const onRequestSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await resetRequestMutation.mutateAsync({ identity: identity.trim() });
      setCanConfirmReset(true);
      setOutput("Request received. Enter and confirm a new password to complete account recovery.");
    } catch (error) {
      setOutput(`Recovery request failed: ${(error as Error).message}`);
    }
  };

  const onConfirmSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (password !== passwordConfirmation) {
      setOutput("Password confirmation mismatch.");
      return;
    }

    try {
      await resetConfirmMutation.mutateAsync({
        identity: identity.trim(),
        password,
        passwordConfirmation
      });
      setPassword("");
      setPasswordConfirmation("");
      setOutput("Account access reset complete. You can now sign in with your new password.");
    } catch (error) {
      setOutput(`Unable to reset account access: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Reset account access</h2>
      <p>Submit your registered identity, then set a new password to complete account recovery.</p>
      <form onSubmit={onRequestSubmit} className="form">
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
          <button type="submit" disabled={resetRequestMutation.isPending}>
            {resetRequestMutation.isPending ? "Submitting..." : "Request Reset"}
          </button>
        </div>
      </form>

      {canConfirmReset ? (
        <form onSubmit={onConfirmSubmit} className="form">
          <label>
            New password
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              minLength={8}
              placeholder="Enter new password"
              required
            />
          </label>
          <label>
            Confirm new password
            <input
              value={passwordConfirmation}
              onChange={(event) => setPasswordConfirmation(event.target.value)}
              type="password"
              minLength={8}
              placeholder="Re-enter new password"
              required
            />
          </label>
          <div className="actions">
            <button type="submit" disabled={resetConfirmMutation.isPending}>
              {resetConfirmMutation.isPending ? "Resetting..." : "Set New Password"}
            </button>
          </div>
        </form>
      ) : null}

      <div className="auth-links">
        <strong>Account access</strong>
        <Link to="/security/login">Back to sign in</Link>
      </div>
      <pre className="output">{output}</pre>
    </article>
  );
}
