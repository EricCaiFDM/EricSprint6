import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  createCustomerProfile,
  type CreateCustomerProfileInput
} from "../services/customers";
import {
  PHONE_NUMBER_MAX_LENGTH,
  validatePhoneNumber
} from "../utils/phoneValidation";

const initialForm: CreateCustomerProfileInput = {
  externalCustomerKey: "",
  legalName: "",
  primaryEmail: "",
  phoneNumber: ""
};

export function CreateCustomerPage() {
  const [form, setForm] = useState<CreateCustomerProfileInput>(initialForm);
  const [output, setOutput] = useState(
    "After signing in, create your customer profile to unlock accounts and payments."
  );

  const createMutation = useMutation({
    mutationFn: createCustomerProfile,
    onSuccess: (profile) => {
      setOutput(
        `Customer profile created successfully. Customer ID: ${profile.customerId}. You can now open an account from the Accounts page.`
      );
      setForm({
        ...initialForm,
        primaryEmail: profile.email,
        legalName: profile.fullName
      });
    },
    onError: (error) => {
      const message = (error as Error).message;
      if (message === "Request failed") {
        setOutput("Unable to create customer profile. Sign in first, then submit this form again.");
        return;
      }
      setOutput(`Customer profile creation failed: ${message}`);
    }
  });

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const phoneValidationError = validatePhoneNumber(form.phoneNumber, true);
    if (phoneValidationError) {
      setOutput(phoneValidationError);
      return;
    }

    await createMutation.mutateAsync({
      ...form,
      phoneNumber: form.phoneNumber.trim()
    }).catch(() => {
      // Error text is set in onError handler.
    });
  };

  return (
    <article className="auth-card">
      <h2>Create customer profile</h2>
      <p>
        This is a dedicated onboarding step. It creates your customer record used by accounts,
        statements, and payments.
      </p>
      <form onSubmit={onSubmit} className="form">
        <label>
          External customer key
          <input
            value={form.externalCustomerKey}
            onChange={(event) =>
              setForm({
                ...form,
                externalCustomerKey: event.target.value
              })
            }
            placeholder="e.g. ext-jordan-001"
            maxLength={120}
            required
          />
        </label>
        <label>
          Legal name
          <input
            value={form.legalName}
            onChange={(event) => setForm({ ...form, legalName: event.target.value })}
            placeholder="Jordan Patel"
            maxLength={160}
            required
          />
        </label>
        <label>
          Email address
          <input
            value={form.primaryEmail}
            onChange={(event) => setForm({ ...form, primaryEmail: event.target.value })}
            type="email"
            placeholder="you@northbridgebank.com"
            maxLength={255}
            required
          />
        </label>
        <label>
          Mobile number
          <input
            value={form.phoneNumber}
            onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })}
            placeholder="+61 412 345 678"
            maxLength={PHONE_NUMBER_MAX_LENGTH}
            required
          />
        </label>
        <div className="actions">
          <button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Creating..." : "Create customer"}
          </button>
        </div>
      </form>
      <div className="auth-links">
        <strong>Onboarding steps</strong>
        <Link to="/security/register">1. Create access profile</Link>
        <Link to="/security/login">2. Sign in</Link>
        <Link to="/customer/accounts">3. Open account</Link>
      </div>
      <pre className="output">{output}</pre>
    </article>
  );
}
