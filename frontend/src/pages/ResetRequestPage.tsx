import { FormEvent, useState } from "react";
import { requestPasswordReset } from "../services/api";

export function ResetRequestPage() {
  const [identity, setIdentity] = useState("");
  const [output, setOutput] = useState("No request yet.");

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const data = await requestPasswordReset({ identity });
      setOutput(`Reset request accepted: ${JSON.stringify(data)}`);
    } catch (error) {
      setOutput(`Reset request failed: ${(error as Error).message}`);
    }
  };

  return (
    <div>
      <h2>Password Reset Request</h2>
      <form onSubmit={onSubmit} className="form">
        <label>
          Identity (Email)
          <input value={identity} onChange={(e) => setIdentity(e.target.value)} type="email" required />
        </label>
        <button type="submit">Request Reset</button>
      </form>
      <pre className="output">{output}</pre>
    </div>
  );
}
