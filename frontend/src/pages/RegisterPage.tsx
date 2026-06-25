import { FormEvent, useState } from "react";
import { register } from "../services/api";

export function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [output, setOutput] = useState("No request yet.");

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await register({ email, password, passwordConfirmation: password });
      setOutput(`Register success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Register failed: ${(error as Error).message}`);
    }
  };

  return (
    <div>
      <h2>Register</h2>
      <form onSubmit={onSubmit} className="form">
        <label>
          Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Password
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" minLength={8} required />
        </label>
        <button type="submit">Create Account</button>
      </form>
      <pre className="output">{output}</pre>
    </div>
  );
}
