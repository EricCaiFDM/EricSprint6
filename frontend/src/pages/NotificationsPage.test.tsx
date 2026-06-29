import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { NotificationsPage } from "./NotificationsPage";
import * as notificationsService from "../services/notifications";

jest.mock("../services/notifications");

describe("NotificationsPage", () => {
  function renderPage() {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false
        }
      }
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();

    (notificationsService.fetchNotificationPreferences as jest.MockedFunction<typeof notificationsService.fetchNotificationPreferences>).mockResolvedValue({
      pushEnabled: true,
      emailEnabled: true,
      smsEnabled: false,
      marketingEnabled: false
    });

    (notificationsService.fetchRecentNotifications as jest.MockedFunction<typeof notificationsService.fetchRecentNotifications>).mockResolvedValue([
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-06-25T10:00:00Z",
        level: "Info"
      }
    ]);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("renders recent alerts from feed", async () => {
    renderPage();

    expect(await screen.findByText(/Deposit posted/i)).toBeInTheDocument();
    expect(screen.getByText(/Delivered successfully/i)).toBeInTheDocument();
  });

  it("shows a live in-page alert when a new feed item arrives", async () => {
    jest.useFakeTimers();

    const feedMock = notificationsService.fetchRecentNotifications as jest.MockedFunction<typeof notificationsService.fetchRecentNotifications>;
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-06-25T10:00:00Z",
        level: "Info"
      }
    ]);
    feedMock.mockResolvedValueOnce([
      {
        notificationId: "notif-2",
        title: "Transfer completed",
        message: "Delivered successfully",
        occurredAt: "2026-06-25T10:00:05Z",
        level: "Info"
      },
      {
        notificationId: "notif-1",
        title: "Deposit posted",
        message: "Delivered successfully",
        occurredAt: "2026-06-25T10:00:00Z",
        level: "Info"
      }
    ]);

    renderPage();

    expect(await screen.findByText(/Deposit posted/i)).toBeInTheDocument();

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
});
