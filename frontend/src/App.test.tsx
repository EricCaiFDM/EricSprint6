import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "./App";
import * as api from "./services/api";

jest.mock("./services/api");

function createMockJwt(claims: Record<string, string>): string {
  const payload = window
    .btoa(JSON.stringify(claims))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}

function renderApp(initialPath = "/") {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("App", () => {
  beforeEach(() => {
    window.localStorage.clear();
    jest.clearAllMocks();
  });

  it("renders public navigation when signed out", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    renderApp();

    expect(screen.getByRole("heading", { name: /Everyday banking, designed for clarity/i })).toBeInTheDocument();
    expect(await screen.findByText(/All banking services are available/i)).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: /^Sign In$/i }).length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: /^Register$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Admin Pages$/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^Log out$/i })).not.toBeInTheDocument();
  });

  it("renders customer navigation for CUSTOMER role", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-001",
        email: "customer@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp();

    expect(await screen.findByRole("link", { name: /^Accounts$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Profile$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Admin Pages$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Log out$/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /^Admin Dashboard$/i })).not.toBeInTheDocument();
  });

  it("renders admin navigation for ADMIN role", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-001",
        email: "admin@example.com",
        role: "ADMIN"
      })
    );

    renderApp();

    expect(await screen.findByRole("heading", { name: /^Admin workspace$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Admin Dashboard$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Log out$/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /^Admin Pages$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /^Accounts$/i })).not.toBeInTheDocument();
  });

  it("logs out and returns to sign-in navigation", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-logout",
        email: "logout@example.com",
        role: "CUSTOMER"
      })
    );
    window.localStorage.setItem("nb_refresh_token", "refresh-token");
    window.localStorage.setItem("nb_customer_id", "customer-logout");

    renderApp("/customer/dashboard");

    fireEvent.click(await screen.findByRole("button", { name: /^Log out$/i }));

    await waitFor(() => {
      expect(window.localStorage.getItem("nb_access_token")).toBeNull();
      expect(window.localStorage.getItem("nb_refresh_token")).toBeNull();
      expect(window.localStorage.getItem("nb_customer_id")).toBeNull();
    });

    expect(screen.getAllByRole("link", { name: /^Sign In$/i }).length).toBeGreaterThan(0);
  });
});
