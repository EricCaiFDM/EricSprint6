import { FormEvent, useState } from "react";
import { refreshToken } from "../services/api";

export function TokenRefreshPage() {
  const [token, setToken] = useState("");
  const [output, setOutput] = useState("No request yet.");

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await refreshToken({ refreshToken: token });
      setOutput(`Refresh success: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Refresh failed: ${(error as Error).message}`);
    }
  };

  return (
    <div>
      <h2>Token Refresh</h2>
      <form onSubmit={onSubmit} className="form">
        <label>
          Refresh Token
          <input value={token} onChange={(e) => setToken(e.target.value)} type="text" required />
        </label>
        <button type="submit">Refresh Token</button>
      </form>
      <pre className="output">{output}</pre>
    </div>
  );
}
