import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "./App";
import * as api from "./services/api";
import * as notifications from "./services/notifications";

jest.mock("./services/api");
jest.mock("./services/notifications");

const actualNotifications = jest.requireActual("./services/notifications") as typeof notifications;

function createMockJwt(claims: Record<string, unknown>): string {
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

    (notifications.isNotificationEnabledByPreferences as jest.MockedFunction<typeof notifications.isNotificationEnabledByPreferences>)
      .mockImplementation(actualNotifications.isNotificationEnabledByPreferences);

    (notifications.fetchNotificationPreferences as jest.MockedFunction<typeof notifications.fetchNotificationPreferences>).mockResolvedValue({
      depositAlertsEnabled: true,
      withdrawalAlertsEnabled: true,
      transferAlertsEnabled: true,
      statementAlertsEnabled: true,
      offersEnabled: false
    });

    (notifications.fetchRecentNotifications as jest.MockedFunction<typeof notifications.fetchRecentNotifications>).mockResolvedValue([]);
  });

  afterEach(() => {
    jest.useRealTimers();
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
    expect(screen.queryByRole("link", { name: /^Admin Pages$/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Log out$/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /^Admin Dashboard$/i })).not.toBeInTheDocument();
  });

  it("toggles the burger menu open and closed", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-nav-toggle",
        email: "customer.nav@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp("/customer/dashboard");

    const menuButton = await screen.findByRole("button", { name: /^Menu$/i });
    expect(menuButton).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(menuButton);

    const closeMenuButton = screen.getByRole("button", { name: /^Close menu$/i });
    expect(closeMenuButton).toHaveAttribute("aria-expanded", "true");

    fireEvent.click(closeMenuButton);

    expect(screen.getByRole("button", { name: /^Menu$/i })).toHaveAttribute("aria-expanded", "false");
  });

  it("shows an on-screen alert when a newer customer notification arrives", async () => {
    jest.useFakeTimers();

    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    const feedMock = notifications.fetchRecentNotifications as jest.MockedFunction<typeof notifications.fetchRecentNotifications>;
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:00Z",
        level: "Info"
      }
    ]);
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-2",
        title: "Transfer completed",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:05Z",
        level: "Info"
      },
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:00Z",
        level: "Info"
      }
    ]);

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-global-alert",
        email: "customer.alert@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp("/customer/dashboard");

    expect(await screen.findByRole("heading", { name: /^Dashboard$/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(feedMock).toHaveBeenCalledTimes(1);
    });

    await act(async () => {
      jest.advanceTimersByTime(5000);
    });

    await waitFor(() => {
      expect(feedMock).toHaveBeenCalledTimes(2);
    });

    expect(await screen.findByRole("alert")).toHaveTextContent("New alert: Transfer completed");

    fireEvent.click(screen.getByRole("button", { name: /Dismiss/i }));

    await waitFor(() => {
      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });
  });

  it("does not show a global snackbar when customer is on payments", async () => {
    jest.useFakeTimers();

    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    const feedMock = notifications.fetchRecentNotifications as jest.MockedFunction<typeof notifications.fetchRecentNotifications>;
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:00Z",
        level: "Info"
      }
    ]);
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-2",
        title: "Transfer completed",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:05Z",
        level: "Info"
      },
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:00Z",
        level: "Info"
      }
    ]);

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-payments-alert",
        email: "customer.payments.alert@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp("/customer/payments");

    expect(await screen.findByRole("heading", { name: /^Payments & transactions$/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(feedMock).toHaveBeenCalledTimes(1);
    });

    await act(async () => {
      jest.advanceTimersByTime(5000);
    });

    await waitFor(() => {
      expect(feedMock).toHaveBeenCalledTimes(2);
    });

    expect(screen.queryByText("New alert: Transfer completed")).not.toBeInTheDocument();
  });

  it("does not show global snackbar for disabled offers notifications", async () => {
    jest.useFakeTimers();

    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    const preferencesMock = notifications.fetchNotificationPreferences as jest.MockedFunction<typeof notifications.fetchNotificationPreferences>;
    preferencesMock.mockResolvedValue({
      depositAlertsEnabled: true,
      withdrawalAlertsEnabled: true,
      transferAlertsEnabled: true,
      statementAlertsEnabled: true,
      offersEnabled: false
    });

    const feedMock = notifications.fetchRecentNotifications as jest.MockedFunction<typeof notifications.fetchRecentNotifications>;
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-10",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:00Z",
        level: "Info"
      }
    ]);
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-11",
        title: "Special offer available",
        message: "Marketing promotion",
        occurredAt: "2026-07-01T10:00:05Z",
        level: "Info"
      },
      {
        notificationId: "notif-10",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-07-01T10:00:00Z",
        level: "Info"
      }
    ]);

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-global-offers-disabled",
        email: "customer.global.offers.disabled@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp("/customer/dashboard");

    expect(await screen.findByRole("heading", { name: /^Dashboard$/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(feedMock).toHaveBeenCalledTimes(1);
    });

    await act(async () => {
      jest.advanceTimersByTime(5000);
    });

    await waitFor(() => {
      expect(feedMock).toHaveBeenCalledTimes(2);
    });

    expect(screen.queryByText("New alert: Special offer available")).not.toBeInTheDocument();
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
    expect(screen.getByRole("link", { name: /^Accounts$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Payments$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Statements$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^Customers$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Log out$/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /^Admin Pages$/i })).not.toBeInTheDocument();
  });

  it("allows admin to access account, payments, statements, and customer routes", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-101",
        email: "admin101@example.com",
        role: "ADMIN"
      })
    );

    renderApp("/admin/accounts");
    expect(await screen.findByRole("heading", { name: /^Account management$/i })).toBeInTheDocument();

    renderApp("/admin/payments");
    expect(await screen.findByRole("heading", { name: /^Payments & transactions$/i })).toBeInTheDocument();

    renderApp("/admin/statements");
    expect(await screen.findByRole("heading", { name: /^Statements$/i })).toBeInTheDocument();

    renderApp("/admin/profile");
    expect(await screen.findByRole("heading", { name: /^Customer management$/i })).toBeInTheDocument();
  });

  it("shows the right rail on non-statements pages", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-rail",
        email: "customer.rail@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp("/customer/dashboard");

    expect(await screen.findByRole("heading", { name: /^Need help\?$/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Accounts & security$/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Trust at NorthBridge$/i })).toBeInTheDocument();
  });

  it("shows support cards on statements pages", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "customer-statements",
        email: "customer.statements@example.com",
        role: "CUSTOMER"
      })
    );

    renderApp("/customer/statements");

    expect(await screen.findByRole("heading", { name: /^Statements$/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Need help\?$/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Accounts & security$/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Trust at NorthBridge$/i })).toBeInTheDocument();
  });

  it("shows session warning modal when 3-minute threshold is reached", async () => {
    jest.useFakeTimers();
    const now = new Date("2026-07-02T08:00:00Z");
    jest.setSystemTime(now);

    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-expiry-warning",
        email: "admin.expiry.warning@example.com",
        role: "ADMIN",
        exp: Math.floor((now.getTime() + 4 * 60 * 1000) / 1000)
      })
    );
    window.localStorage.setItem("nb_refresh_token", "refresh-warning-token");

    renderApp("/admin/dashboard");

    expect(await screen.findByRole("heading", { name: /^Admin workspace$/i })).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(60 * 1000);
    });

    const dialog = await screen.findByRole("dialog", { name: /Session expiring soon/i });
    expect(dialog).toBeInTheDocument();
    expect(screen.getByText("Your session is about to expire. Stay signed in?")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Stay signed in$/i })).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /^Log out$/i })).toBeInTheDocument();
  });

  it("refreshes session when user chooses stay signed in", async () => {
    jest.useFakeTimers();
    const now = new Date("2026-07-02T09:30:00Z");
    jest.setSystemTime(now);

    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");
    const mockedRefreshToken = api.refreshToken as jest.MockedFunction<typeof api.refreshToken>;
    mockedRefreshToken.mockImplementation(async () => {
      const refreshedToken = createMockJwt({
        sub: "admin-stay-signed-in",
        email: "admin.stay.signed.in@example.com",
        role: "ADMIN",
        exp: Math.floor((Date.now() + 10 * 60 * 1000) / 1000)
      });
      window.localStorage.setItem("nb_access_token", refreshedToken);
      window.localStorage.setItem("nb_refresh_token", "refresh-token-new");
      window.dispatchEvent(new Event("nb-auth-changed"));
      return {
        accessToken: refreshedToken,
        refreshToken: "refresh-token-new",
        expiresIn: 600
      };
    });

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-stay-signed-in",
        email: "admin.stay.signed.in@example.com",
        role: "ADMIN",
        exp: Math.floor((now.getTime() + 4 * 60 * 1000) / 1000)
      })
    );
    window.localStorage.setItem("nb_refresh_token", "refresh-token-old");

    renderApp("/admin/dashboard");
    expect(await screen.findByRole("heading", { name: /^Admin workspace$/i })).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(60 * 1000);
    });

    fireEvent.click(await screen.findByRole("button", { name: /^Stay signed in$/i }));

    await waitFor(() => {
      expect(mockedRefreshToken).toHaveBeenCalledWith({ refreshToken: "refresh-token-old" });
    });

    await waitFor(() => {
      expect(screen.queryByRole("dialog", { name: /Session expiring soon/i })).not.toBeInTheDocument();
    });

    act(() => {
      jest.advanceTimersByTime(3 * 60 * 1000 + 500);
    });

    expect(screen.getByRole("heading", { name: /^Admin workspace$/i })).toBeInTheDocument();
    expect(window.localStorage.getItem("nb_access_token")).not.toBeNull();
  });

  it("logs user out automatically when token expires without response", async () => {
    jest.useFakeTimers();
    const now = new Date("2026-07-02T10:45:00Z");
    jest.setSystemTime(now);

    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    window.localStorage.setItem(
      "nb_access_token",
      createMockJwt({
        sub: "admin-auto-expiry",
        email: "admin.auto.expiry@example.com",
        role: "ADMIN",
        exp: Math.floor((now.getTime() + 4 * 60 * 1000) / 1000)
      })
    );
    window.localStorage.setItem("nb_refresh_token", "refresh-expire-token");
    window.localStorage.setItem("nb_customer_id", "customer-expire-token");

    renderApp("/admin/dashboard");
    expect(await screen.findByRole("heading", { name: /^Admin workspace$/i })).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(60 * 1000);
    });
    expect(await screen.findByRole("dialog", { name: /Session expiring soon/i })).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(3 * 60 * 1000 + 500);
    });

    await waitFor(() => {
      expect(window.localStorage.getItem("nb_access_token")).toBeNull();
      expect(window.localStorage.getItem("nb_refresh_token")).toBeNull();
      expect(window.localStorage.getItem("nb_customer_id")).toBeNull();
    });

    expect(await screen.findByRole("heading", { name: /Sign in to your account/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^Log out$/i })).not.toBeInTheDocument();
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
