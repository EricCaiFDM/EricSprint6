import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { NotificationsPage } from "./NotificationsPage";
import * as notificationsService from "../services/notifications";

jest.mock("../services/notifications");

const actualNotificationsService = jest.requireActual("../services/notifications") as typeof notificationsService;

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

    (notificationsService.isNotificationEnabledByPreferences as jest.MockedFunction<typeof notificationsService.isNotificationEnabledByPreferences>)
      .mockImplementation(actualNotificationsService.isNotificationEnabledByPreferences);

    (notificationsService.fetchNotificationPreferences as jest.MockedFunction<typeof notificationsService.fetchNotificationPreferences>).mockResolvedValue({
      depositAlertsEnabled: true,
      withdrawalAlertsEnabled: true,
      transferAlertsEnabled: true,
      statementAlertsEnabled: true,
      offersEnabled: false
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

    (notificationsService.updateNotificationPreferences as jest.MockedFunction<typeof notificationsService.updateNotificationPreferences>).mockResolvedValue({
      depositAlertsEnabled: true,
      withdrawalAlertsEnabled: true,
      transferAlertsEnabled: true,
      statementAlertsEnabled: true,
      offersEnabled: false
    });
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

  it("does not show live alert for topics that are disabled", async () => {
    jest.useFakeTimers();

    const preferencesMock = notificationsService.fetchNotificationPreferences as jest.MockedFunction<typeof notificationsService.fetchNotificationPreferences>;
    preferencesMock.mockResolvedValue({
      depositAlertsEnabled: true,
      withdrawalAlertsEnabled: true,
      transferAlertsEnabled: true,
      statementAlertsEnabled: true,
      offersEnabled: false
    });

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
        notificationId: "notif-3",
        title: "Special offer available",
        message: "Marketing promotion",
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

    expect(screen.queryByText("New alert: Special offer available")).not.toBeInTheDocument();
  });

  it("shows an alert when preferences are saved", async () => {
    const updatePreferencesMock = notificationsService.updateNotificationPreferences as jest.MockedFunction<typeof notificationsService.updateNotificationPreferences>;

    renderPage();

    const transferUpdatesCheckbox = await screen.findByLabelText(/Transfer updates/i);
    await waitFor(() => {
      expect(transferUpdatesCheckbox).toBeChecked();
    });

    fireEvent.click(transferUpdatesCheckbox);
    expect(transferUpdatesCheckbox).not.toBeChecked();

    const saveButton = await screen.findByRole("button", { name: /Save preferences/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(updatePreferencesMock).toHaveBeenCalled();
    });

    const submittedPreferences = updatePreferencesMock.mock.calls[0]?.[0];
    expect(submittedPreferences).toEqual({
      depositAlertsEnabled: true,
      withdrawalAlertsEnabled: true,
      transferAlertsEnabled: false,
      statementAlertsEnabled: true,
      offersEnabled: false
    });

    expect(await screen.findByRole("alert")).toHaveTextContent(/Preferences have been updated\./i);
  });
});
