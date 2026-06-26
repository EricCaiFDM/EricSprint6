import { apiClient } from "./api";
import { requireCustomerId, setActiveCustomerId } from "./session";

export type CustomerProfile = {
  customerId: string;
  fullName: string;
  email: string;
  mobile: string;
  status: "ACTIVE" | "SUSPENDED" | "CLOSED";
  joinedAt: string;
};

export type CustomerContactUpdate = {
  phoneNumber: string;
};

export type CreateCustomerProfileInput = {
  externalCustomerKey: string;
  legalName: string;
  primaryEmail: string;
  phoneNumber: string;
};

export async function createCustomerProfile(input: CreateCustomerProfileInput): Promise<CustomerProfile> {
  const response = await apiClient.post("/customers", {
    externalCustomerKey: input.externalCustomerKey,
    legalName: input.legalName,
    primaryEmail: input.primaryEmail,
    phoneNumber: input.phoneNumber
  });

  const profile = mapCustomerProfile(response.data);
  setActiveCustomerId(profile.customerId);
  return profile;
}

export async function fetchCustomerProfile(): Promise<CustomerProfile> {
  const customerId = requireCustomerId();
  const response = await apiClient.get(`/customers/${encodeURIComponent(customerId)}`);
  const profile = mapCustomerProfile(response.data);
  setActiveCustomerId(profile.customerId);
  return profile;
}

export async function updateCustomerContact(update: CustomerContactUpdate): Promise<CustomerProfile> {
  const customerId = requireCustomerId();
  const response = await apiClient.patch(`/customers/${encodeURIComponent(customerId)}`, {
    phoneNumber: update.phoneNumber
  });
  const profile = mapCustomerProfile(response.data);
  setActiveCustomerId(profile.customerId);
  return profile;
}

function mapCustomerProfile(data: unknown): CustomerProfile {
  if (!data || typeof data !== "object") {
    throw new Error("Customer profile payload is invalid");
  }

  const row = data as Record<string, unknown>;
  const customerId = asString(row.customerId, "");
  if (!customerId) {
    throw new Error("Customer profile is missing customerId");
  }

  return {
    customerId,
    fullName: asString(row.legalName, "Customer"),
    email: asString(row.primaryEmail, ""),
    mobile: asString(row.phoneNumber, ""),
    status: asStatus(row.status),
    joinedAt: asString(row.createdAtUtc, new Date().toISOString())
  };
}

function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asStatus(value: unknown): CustomerProfile["status"] {
  if (value === "SUSPENDED") {
    return "SUSPENDED";
  }
  if (value === "CLOSED") {
    return "CLOSED";
  }
  return "ACTIVE";
}
