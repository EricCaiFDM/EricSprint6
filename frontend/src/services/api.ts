import axios, { AxiosError } from "axios";

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
  return parseApiResponse(apiClient.post("/auth/login", payload));
}

export async function requestPasswordReset(payload: PasswordResetRequest): Promise<GenericAcknowledgeResponse> {
  return parseApiResponse(apiClient.post("/auth/password-reset/request", payload));
}

export async function refreshToken(payload: RefreshRequest): Promise<RefreshResponse> {
  return parseApiResponse(apiClient.post("/auth/token/refresh", payload));
}

async function parseApiResponse<T>(request: Promise<{ data: T }>): Promise<T> {
  try {
    const response = await request;
    return response.data;
  } catch (error) {
    const axiosError = error as AxiosError<{ message?: string }>;
    const message =
      axiosError.response?.data?.message && typeof axiosError.response.data.message === "string"
        ? axiosError.response.data.message
        : "Request failed";
    throw new Error(message);
  }
}
