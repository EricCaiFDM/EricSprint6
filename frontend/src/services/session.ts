const ACCESS_TOKEN_KEY = "nb_access_token";
const REFRESH_TOKEN_KEY = "nb_refresh_token";
const CUSTOMER_ID_KEY = "nb_customer_id";

type JwtClaims = {
  sub?: string;
  email?: string;
  role?: string;
};

export type UserRole = "ADMIN" | "CUSTOMER";

function hasStorage(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readStorage(key: string): string | null {
  if (!hasStorage()) {
    return null;
  }
  return window.localStorage.getItem(key);
}

function writeStorage(key: string, value: string): void {
  if (!hasStorage()) {
    return;
  }
  window.localStorage.setItem(key, value);
}

function notifyAuthChanged(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new Event("nb-auth-changed"));
}

export function saveAuthSession(accessToken: string, refreshToken: string): void {
  if (hasStorage()) {
    window.localStorage.removeItem(CUSTOMER_ID_KEY);
  }
  writeStorage(ACCESS_TOKEN_KEY, accessToken);
  writeStorage(REFRESH_TOKEN_KEY, refreshToken);
  notifyAuthChanged();
}

export function clearAuthSession(): void {
  if (!hasStorage()) {
    return;
  }
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.localStorage.removeItem(CUSTOMER_ID_KEY);
  notifyAuthChanged();
}

export function getAccessToken(): string | null {
  return readStorage(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return readStorage(REFRESH_TOKEN_KEY);
}

export function setActiveCustomerId(customerId: string): void {
  if (customerId.trim().length === 0) {
    return;
  }
  writeStorage(CUSTOMER_ID_KEY, customerId);
}

export function getActiveCustomerId(): string | null {
  const saved = readStorage(CUSTOMER_ID_KEY);
  if (saved && saved.trim().length > 0) {
    return saved;
  }

  return null;
}

export function requireCustomerId(): string {
  const customerId = getActiveCustomerId();
  if (!customerId) {
    throw new Error("No customer context found. Sign in before opening customer pages.");
  }
  return customerId;
}

export function getTokenSubject(): string | null {
  return getTokenClaims()?.sub ?? null;
}

export function getTokenEmail(): string | null {
  return getTokenClaims()?.email ?? null;
}

export function getTokenRole(): string | null {
  return getTokenClaims()?.role ?? null;
}

export function getNormalizedTokenRole(): UserRole | null {
  const role = getTokenRole();
  if (!role) {
    return null;
  }

  const normalized = role.trim().toUpperCase();
  if (normalized === "ADMIN" || normalized === "CUSTOMER") {
    return normalized;
  }

  return null;
}

function getTokenClaims(): JwtClaims | null {
  const token = getAccessToken();
  if (!token) {
    return null;
  }

  const segments = token.split(".");
  if (segments.length < 2) {
    return null;
  }

  try {
    const decoded = decodeBase64Url(segments[1]);
    const claims = JSON.parse(decoded) as JwtClaims;
    return claims;
  } catch {
    return null;
  }
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  return atob(padded);
}
