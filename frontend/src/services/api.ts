export type RegisterRequest = {
  email: string;
  password: string;
  passwordConfirmation: string;
};

export type LoginRequest = {
  identity: string;
  password: string;
};

export type PasswordResetRequest = {
  identity: string;
};

export type RefreshRequest = {
  refreshToken: string;
};

export async function checkHealth(): Promise<string> {
  const response = await fetch("/api/health");
  if (!response.ok) {
    throw new Error("Health endpoint failed");
  }
  return response.text();
}

export async function register(payload: RegisterRequest): Promise<unknown> {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return parseJson(response);
}

export async function login(payload: LoginRequest): Promise<unknown> {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return parseJson(response);
}

export async function requestPasswordReset(payload: PasswordResetRequest): Promise<unknown> {
  const response = await fetch("/api/auth/password-reset/request", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return parseJson(response);
}

export async function refreshToken(payload: RefreshRequest): Promise<unknown> {
  const response = await fetch("/api/auth/token/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return parseJson(response);
}

async function parseJson(response: Response): Promise<unknown> {
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(typeof body === "object" && body !== null && "message" in body ? String((body as { message: unknown }).message) : "Request failed");
  }
  return body;
}
