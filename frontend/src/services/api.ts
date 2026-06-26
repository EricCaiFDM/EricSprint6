import axios, { AxiosError, AxiosHeaders } from "axios";
import { clearAuthSession, getAccessToken, saveAuthSession } from "./session";

type ApiErrorBody = {
  message?: string;
  code?: string;
  field?: string;
};

export type ApiErrorDetails = {
  status?: number;
  code?: string;
  field?: string;
  message: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  passwordConfirmation: string;
};

export type RegisterResponse = {
  status: "CREATED";
  userId: string;
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

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

export type GenericAcknowledgeResponse = {
  status: "ACCEPTED";
  message: string;
};

export type RefreshResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

export const apiClient = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json"
  }
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (!token) {
    return config;
  }

  if (!config.headers) {
    config.headers = new AxiosHeaders();
  }

  if (typeof (config.headers as { set?: (key: string, value: string) => void }).set === "function") {
    (config.headers as { set: (key: string, value: string) => void }).set("Authorization", `Bearer ${token}`);
  } else {
    (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = (error as AxiosError).response?.status;
    if (status === 401) {
      clearAuthSession();
    }
    return Promise.reject(error);
  }
);

export async function checkHealth(): Promise<string> {
  try {
    const response = await apiClient.get<string>("/health", { responseType: "text" });
    return response.data;
  } catch {
    throw new Error("Health endpoint failed");
  }
}

export async function register(payload: RegisterRequest): Promise<RegisterResponse> {
  return parseApiResponse(apiClient.post("/auth/register", payload));
}

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const response = await parseApiResponse<LoginResponse>(apiClient.post("/auth/login", payload));
  if (!response.accessToken || !response.refreshToken) {
    throw new Error("Authentication response missing required tokens");
  }
  saveAuthSession(response.accessToken, response.refreshToken);
  return response;
}

export async function requestPasswordReset(payload: PasswordResetRequest): Promise<GenericAcknowledgeResponse> {
  return parseApiResponse(apiClient.post("/auth/password-reset/request", payload));
}

export async function refreshToken(payload: RefreshRequest): Promise<RefreshResponse> {
  const response = await parseApiResponse<RefreshResponse>(apiClient.post("/auth/token/refresh", payload));
  if (!response.accessToken || !response.refreshToken) {
    throw new Error("Authentication response missing required tokens");
  }
  saveAuthSession(response.accessToken, response.refreshToken);
  return response;
}

export function getApiErrorDetails(error: unknown, fallbackMessage = "Request failed"): ApiErrorDetails {
  const axiosError = error as AxiosError<ApiErrorBody>;
  const body = axiosError.response?.data;
  const message =
    body?.message && typeof body.message === "string"
      ? body.message
      : typeof axiosError.message === "string" && axiosError.message.trim().length > 0
        ? axiosError.message
        : fallbackMessage;

  return {
    status: axiosError.response?.status,
    code: body?.code,
    field: body?.field,
    message
  };
}

async function parseApiResponse<T>(request: Promise<{ data: T }>): Promise<T> {
  try {
    const response = await request;
    return response.data;
  } catch (error) {
    const details = getApiErrorDetails(error);
    throw new Error(details.message);
  }
}
