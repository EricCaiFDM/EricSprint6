import { FormEvent, useState } from "react";
import { login, register } from "../services/api";

export function AuthPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [output, setOutput] = useState("No request yet.");

  const onRegister = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await register({ email, password, passwordConfirmation: password });
      setOutput(`Register success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Register failed: ${(error as Error).message}`);
    }
  };

  const onLogin = async () => {
    try {
      const data = await login({ identity: email, password });
      setOutput(`Login success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Login failed: ${(error as Error).message}`);
    }
  };

  return (
    <div>
      <h2>Authentication</h2>
      <form onSubmit={onRegister} className="form">
        <label>
          Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Password
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" minLength={8} required />
        </label>
        <div className="actions">
          <button type="submit">Register</button>
          <button type="button" onClick={onLogin}>Login</button>
        </div>
      </form>
      <pre className="output">{output}</pre>
    </div>
  );
}
