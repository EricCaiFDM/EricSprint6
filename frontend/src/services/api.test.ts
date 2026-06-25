import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  checkHealth,
  login,
  refreshToken,
  register,
  requestPasswordReset
} from "./api";

describe("api service", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("checkHealth returns endpoint text when successful", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response("OK", { status: 200 })
    );

    const result = await checkHealth();

    expect(result).toBe("OK");
    expect(fetchMock).toHaveBeenCalledWith("/api/health");
  });

  it("checkHealth throws when endpoint fails", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("nope", { status: 503 }));

    await expect(checkHealth()).rejects.toThrow("Health endpoint failed");
  });

  it("register posts payload and returns parsed JSON", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ status: "CREATED", userId: "abc" }), {
        status: 201,
        headers: { "Content-Type": "application/json" }
      })
    );

    const payload = {
      email: "jane@example.com",
      password: "secret123",
      passwordConfirmation: "secret123"
    };

    const result = await register(payload);

    expect(result).toEqual({ status: "CREATED", userId: "abc" });
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
  });

  it("login throws backend message on non-2xx response", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "Invalid credentials" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      })
    );

    await expect(login({ identity: "jane@example.com", password: "bad" })).rejects.toThrow(
      "Invalid credentials"
    );
  });

  it("requestPasswordReset posts identity payload", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ status: "ACCEPTED" }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    await requestPasswordReset({ identity: "jane@example.com" });

    expect(fetchMock).toHaveBeenCalledWith("/api/auth/password-reset/request", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identity: "jane@example.com" })
    });
  });

  it("refreshToken posts token payload", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ accessToken: "newAccess" }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    await refreshToken({ refreshToken: "oldRefresh" });

    expect(fetchMock).toHaveBeenCalledWith("/api/auth/token/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: "oldRefresh" })
    });
  });
});
