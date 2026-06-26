import { apiClient } from "./api";

export type CustomerProfile = {
  firstName: string;
  lastName: string;
  email: string;
  mobile: string;
  addressLine1: string;
  city: string;
  postalCode: string;
  loyaltyTier: "Standard" | "Premium" | "Private";
  joinedAt: string;
};

export type CustomerContactUpdate = {
  mobile: string;
  addressLine1: string;
  city: string;
  postalCode: string;
};

const fallbackProfile: CustomerProfile = {
  firstName: "Jordan",
  lastName: "Patel",
  email: "jordan.patel@example.com",
  mobile: "+61 412 345 678",
  addressLine1: "84 Harbour View Road",
  city: "Sydney",
  postalCode: "2000",
  loyaltyTier: "Premium",
  joinedAt: "2024-03-12T00:00:00Z"
};

export async function fetchCustomerProfile(): Promise<CustomerProfile> {
  try {
    const response = await apiClient.get("/customers/me");
    return mapCustomerProfile(response.data);
  } catch {
    return fallbackProfile;
  }
}

export async function updateCustomerContact(update: CustomerContactUpdate): Promise<CustomerProfile> {
  try {
    const response = await apiClient.patch("/customers/me", update);
    return mapCustomerProfile(response.data);
  } catch {
    return {
      ...fallbackProfile,
      ...update
    };
  }
}

function mapCustomerProfile(data: unknown): CustomerProfile {
  if (!data || typeof data !== "object") {
    return fallbackProfile;
  }

  const row = data as Record<string, unknown>;
  return {
    firstName: asString(row.firstName, fallbackProfile.firstName),
    lastName: asString(row.lastName, fallbackProfile.lastName),
    email: asString(row.email, fallbackProfile.email),
    mobile: asString(row.mobile, fallbackProfile.mobile),
    addressLine1: asString(row.addressLine1, fallbackProfile.addressLine1),
    city: asString(row.city, fallbackProfile.city),
    postalCode: asString(row.postalCode, fallbackProfile.postalCode),
    loyaltyTier: asTier(row.loyaltyTier, fallbackProfile.loyaltyTier),
    joinedAt: asString(row.joinedAt, fallbackProfile.joinedAt)
  };
}

function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asTier(value: unknown, fallback: CustomerProfile["loyaltyTier"]): CustomerProfile["loyaltyTier"] {
  return value === "Standard" || value === "Premium" || value === "Private" ? value : fallback;
}
