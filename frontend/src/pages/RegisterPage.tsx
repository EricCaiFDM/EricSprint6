import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { register } from "../services/api";

export function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [output, setOutput] = useState("Create your profile to start using digital banking.");
  const registerMutation = useMutation({
    mutationFn: register
  });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await registerMutation.mutateAsync({
        email,
        password,
        passwordConfirmation: password
      });
      setOutput("Profile created. You can now sign in securely.");
    } catch (error) {
      setOutput(`Profile setup failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Open a digital access profile</h2>
      <p>
        Dedicated access-profile page. Register here first, then sign in, then create your customer profile.
      </p>
      <form onSubmit={onSubmit} className="form">
        <label>
          Email address
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            placeholder="you@northbridgebank.com"
            required
          />
        </label>
        <label>
          Password (min 8 characters)
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            minLength={8}
            placeholder="Create a strong password"
            required
          />
        </label>
        <div className="actions">
          <button type="submit" disabled={registerMutation.isPending}>
            {registerMutation.isPending ? "Creating..." : "Create Profile"}
          </button>
        </div>
      </form>
      <div className="auth-links">
        <strong>Next steps</strong>
        <Link to="/security/login">Sign in</Link>
        <Link to="/security/create-customer">Create customer profile</Link>
      </div>
      <pre className="output">{output}</pre>
    </article>
  );
}
