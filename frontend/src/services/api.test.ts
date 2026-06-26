import { beforeEach, describe, expect, it, jest } from "@jest/globals";
import {
  apiClient,
  checkHealth,
  login,
  refreshToken,
  register,
  requestPasswordReset
} from "./api";

describe("api service", () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    window.localStorage.clear();
  });

  it("checkHealth returns endpoint text when successful", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({ data: "OK" } as never);

    const result = await checkHealth();

    expect(result).toBe("OK");
    expect(getMock).toHaveBeenCalledWith("/health", { responseType: "text" });
  });

  it("checkHealth throws when endpoint fails", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue(new Error("nope"));

    await expect(checkHealth()).rejects.toThrow("Health endpoint failed");
  });

  it("register posts payload and returns parsed JSON", async () => {
    const postMock = jest
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: { status: "CREATED", userId: "abc" } } as never);

    const payload = {
      email: "jane@example.com",
      password: "secret123",
      passwordConfirmation: "secret123"
    };

    const result = await register(payload);

    expect(result).toEqual({ status: "CREATED", userId: "abc" });
    expect(postMock).toHaveBeenCalledWith("/auth/register", payload);
  });

  it("login throws backend message on non-2xx response", async () => {
    jest.spyOn(apiClient, "post").mockRejectedValue({
      response: {
        data: {
          message: "Invalid credentials"
        }
      }
    });

    await expect(login({ identity: "jane@example.com", password: "bad" })).rejects.toThrow(
      "Invalid credentials"
    );
  });

  it("requestPasswordReset posts identity payload", async () => {
    const postMock = jest
      .spyOn(apiClient, "post")
      .mockResolvedValue({ data: { status: "ACCEPTED" } } as never);

    await requestPasswordReset({ identity: "jane@example.com" });

    expect(postMock).toHaveBeenCalledWith("/auth/password-reset/request", {
      identity: "jane@example.com"
    });
  });

  it("refreshToken posts token payload", async () => {
    const postMock = jest
      .spyOn(apiClient, "post")
      .mockResolvedValue({
        data: { accessToken: "newAccess", refreshToken: "newRefresh", expiresIn: 3600 }
      } as never);

    await refreshToken({ refreshToken: "oldRefresh" });

    expect(postMock).toHaveBeenCalledWith("/auth/token/refresh", {
      refreshToken: "oldRefresh"
    });
    expect(window.localStorage.getItem("nb_access_token")).toBe("newAccess");
    expect(window.localStorage.getItem("nb_refresh_token")).toBe("newRefresh");
  });

  it("login stores access and refresh tokens", async () => {
    jest.spyOn(apiClient, "post").mockResolvedValue({
      data: { accessToken: "access-1", refreshToken: "refresh-1", expiresIn: 3600 }
    } as never);

    await login({ identity: "jane@example.com", password: "secret123" });

    expect(window.localStorage.getItem("nb_access_token")).toBe("access-1");
    expect(window.localStorage.getItem("nb_refresh_token")).toBe("refresh-1");
  });
});
