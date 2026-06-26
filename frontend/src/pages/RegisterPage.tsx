import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { login, register } from "../services/api";
import { createCustomerProfile } from "../services/customers";
import { clearAuthSession, getNormalizedTokenRole } from "../services/session";

function toExternalCustomerKey(email: string): string {
  const normalized = email
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

  return (`cust-${normalized || "new-user"}`).slice(0, 120);
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"CUSTOMER" | "ADMIN">("CUSTOMER");
  const [legalName, setLegalName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [output, setOutput] = useState("Create an Admin or Customer account.");
  const isCustomer = role === "CUSTOMER";

  const registerMutation = useMutation({
    mutationFn: register
  });
  const loginMutation = useMutation({
    mutationFn: login
  });
  const customerProfileMutation = useMutation({
    mutationFn: createCustomerProfile
  });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (isCustomer && (legalName.trim().length === 0 || phoneNumber.trim().length === 0)) {
      setOutput("Customer accounts require legal name and mobile number.");
      return;
    }

    try {
      await registerMutation.mutateAsync({
        email,
        password,
        passwordConfirmation: password,
        role
      });

      const auth = await loginMutation.mutateAsync({
        identity: email,
        password
      });

      const minutes = Math.round(auth.expiresIn / 60);
      const signedInRole = getNormalizedTokenRole() ?? role;

      if (signedInRole !== role) {
        clearAuthSession();
        setOutput(
          `Access role mismatch. You selected ${role}, but the signed-in account role is ${signedInRole}. ` +
            "Use a different email to register a new admin profile."
        );
        return;
      }

      if (signedInRole === "CUSTOMER") {
        try {
          await customerProfileMutation.mutateAsync({
            externalCustomerKey: toExternalCustomerKey(email),
            legalName: legalName.trim(),
            primaryEmail: email.trim().toLowerCase(),
            phoneNumber: phoneNumber.trim()
          });
        } catch (customerError) {
          clearAuthSession();
          setOutput(
            `Customer account created, but profile setup failed: ${(customerError as Error).message}. ` +
              "Sign in and retry account setup."
          );
          return;
        }

        setOutput(
          `Customer account created and signed in. Session active for approximately ${minutes} minutes.`
        );
        navigate("/customer/dashboard", { replace: true });
      } else {
        setOutput(
          `Admin account created and signed in. Session active for approximately ${minutes} minutes.`
        );
        navigate("/admin/dashboard", { replace: true });
      }
    } catch (error) {
      setOutput(`Profile setup failed: ${(error as Error).message}`);
    }
  };

  return (
    <article className="auth-card">
      <h2>Create your account</h2>
      <p>
        Choose Customer or Admin account type. Customer account setup is completed in this single flow.
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

        <label>
          Account type
          <select value={role} onChange={(e) => setRole(e.target.value as "CUSTOMER" | "ADMIN")}>
            <option value="CUSTOMER">Customer</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>

        {isCustomer ? (
          <>
            <label>
              Legal name
              <input
                value={legalName}
                onChange={(e) => setLegalName(e.target.value)}
                placeholder="Jordan Patel"
                maxLength={160}
                required={isCustomer}
              />
            </label>
            <label>
              Mobile number
              <input
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="+61 412 345 678"
                required={isCustomer}
              />
            </label>
          </>
        ) : null}

        <div className="actions">
          <button
            type="submit"
            disabled={registerMutation.isPending || loginMutation.isPending || customerProfileMutation.isPending}
          >
            {registerMutation.isPending
              ? "Creating..."
              : loginMutation.isPending
                ? "Signing in..."
                : customerProfileMutation.isPending
                  ? "Finalizing..."
                  : "Create Account"}
          </button>
        </div>
      </form>
      <div className="auth-links">
        <strong>Already have an account?</strong>
        <Link to="/security/login">Sign in</Link>
        <Link to="/security/reset">Recover account</Link>
      </div>
      <pre className="output">{output}</pre>
    </article>
  );
}
