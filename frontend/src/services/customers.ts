import { apiClient, getApiErrorDetails } from "./api";
import { clearActiveCustomerId, getActiveCustomerId, setActiveCustomerId } from "./session";

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

export type UpdateCustomerProfileInput = {
  legalName?: string;
  primaryEmail?: string;
  phoneNumber?: string;
  status?: "ACTIVE" | "SUSPENDED" | "CLOSED";
};

export type DeleteCustomerResult = {
  status: "DELETED";
  message: string;
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
  return resolveCurrentCustomerProfile();
}

export async function fetchCustomerDetails(customerId: string): Promise<CustomerProfile> {
  if (!customerId || customerId.trim().length === 0) {
    throw new Error("Enter a valid customer ID.");
  }

  return getCustomerById(customerId.trim());
}

export async function updateCustomerContact(update: CustomerContactUpdate): Promise<CustomerProfile> {
  return updateCustomerProfile({ phoneNumber: update.phoneNumber });
}

export async function updateCustomerProfile(
  update: UpdateCustomerProfileInput,
  customerId?: string
): Promise<CustomerProfile> {
  const targetCustomerId = customerId?.trim() || (await resolveCurrentCustomerProfile()).customerId;
  if (Object.keys(update).length === 0) {
    throw new Error("Provide at least one field to update.");
  }

  const response = await apiClient.patch(`/customers/${encodeURIComponent(targetCustomerId)}`, {
    legalName: update.legalName?.trim() || undefined,
    primaryEmail: update.primaryEmail?.trim() || undefined,
    phoneNumber: update.phoneNumber?.trim() || undefined,
    status: update.status
  });

  const profile = mapCustomerProfile(response.data);
  setActiveCustomerId(profile.customerId);
  return profile;
}

export async function deleteCustomerProfile(customerId?: string): Promise<DeleteCustomerResult> {
  const targetCustomerId = customerId?.trim() || (await resolveCurrentCustomerProfile()).customerId;

  try {
    const response = await apiClient.delete(`/customers/${encodeURIComponent(targetCustomerId)}`);
    if (getActiveCustomerId() === targetCustomerId) {
      clearActiveCustomerId();
    }

    const payload = response.data as Record<string, unknown>;
    const status = asString(payload?.status, "DELETED");
    return {
      status: status === "DELETED" ? "DELETED" : "DELETED",
      message: asString(payload?.message, "Customer deleted")
    };
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to delete the selected customer.");
    }
    if (details.status === 404) {
      throw new Error("The selected customer could not be found.");
    }
    if (details.status === 409) {
      throw new Error("Customer deletion is blocked by account dependencies or retention policy.");
    }
    throw new Error(details.message);
  }
}

export async function resolveCurrentCustomerProfile(): Promise<CustomerProfile> {
  const activeCustomerId = getActiveCustomerId();

  if (activeCustomerId) {
    try {
      return await getCustomerById(activeCustomerId);
    } catch (error) {
      const details = getApiErrorDetails(error);
      if (details.status !== 400 && details.status !== 403 && details.status !== 404) {
        throw new Error(details.message);
      }
    }
  }

  try {
    const response = await apiClient.get("/customers/me");
    const profile = mapCustomerProfile(response.data);
    setActiveCustomerId(profile.customerId);
    return profile;
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.code === "CUSTOMER_NOT_FOUND" || details.status === 404) {
      throw new Error("No customer account record found for this sign-in. Complete account setup first.");
    }
    throw new Error(details.message);
  }
}

async function getCustomerById(customerId: string): Promise<CustomerProfile> {
  const response = await apiClient.get(`/customers/${encodeURIComponent(customerId)}`);
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
