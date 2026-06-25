import { FormEvent, useState } from "react";
import { login } from "../services/api";

export function LoginPage() {
  const [identity, setIdentity] = useState("");
  const [password, setPassword] = useState("");
  const [output, setOutput] = useState("No request yet.");

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await login({ identity, password });
      setOutput(`Login success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Login failed: ${(error as Error).message}`);
    }
  };

  return (
    <div>
      <h2>Login</h2>
      <form onSubmit={onSubmit} className="form">
        <label>
          Identity (Email)
          <input value={identity} onChange={(e) => setIdentity(e.target.value)} type="email" required />
        </label>
        <label>
          Password
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
        </label>
        <button type="submit">Login</button>
      </form>
      <pre className="output">{output}</pre>
    </div>
  );
}
