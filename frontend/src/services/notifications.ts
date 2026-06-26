import { apiClient } from "./api";

export type NotificationPreferences = {
  pushEnabled: boolean;
  emailEnabled: boolean;
  smsEnabled: boolean;
  marketingEnabled: boolean;
};

export type NotificationItem = {
  notificationId: string;
  title: string;
  message: string;
  occurredAt: string;
  level: "Info" | "Warning";
};

const fallbackPreferences: NotificationPreferences = {
  pushEnabled: true,
  emailEnabled: true,
  smsEnabled: false,
  marketingEnabled: false
};

const fallbackNotifications: NotificationItem[] = [
  {
    notificationId: "n-201",
    title: "Card transaction approved",
    message: "Your card payment of AUD 64.50 at Osteria Lane was approved.",
    occurredAt: "2026-06-23T15:45:00Z",
    level: "Info"
  },
  {
    notificationId: "n-202",
    title: "Upcoming scheduled payment",
    message: "A recurring payment of AUD 1,950.00 is due on 01 Jul 2026.",
    occurredAt: "2026-06-25T08:20:00Z",
    level: "Info"
  },
  {
    notificationId: "n-203",
    title: "Low balance alert",
    message: "Your available balance fell below your selected threshold.",
    occurredAt: "2026-06-22T07:18:00Z",
    level: "Warning"
  }
];

export async function fetchNotificationPreferences(): Promise<NotificationPreferences> {
  try {
    const response = await apiClient.get("/notifications/preferences");
    const data = response.data as Record<string, unknown>;
    return {
      pushEnabled: asBoolean(data.pushEnabled, fallbackPreferences.pushEnabled),
      emailEnabled: asBoolean(data.emailEnabled, fallbackPreferences.emailEnabled),
      smsEnabled: asBoolean(data.smsEnabled, fallbackPreferences.smsEnabled),
      marketingEnabled: asBoolean(data.marketingEnabled, fallbackPreferences.marketingEnabled)
    };
  } catch {
    return fallbackPreferences;
  }
}

export async function updateNotificationPreferences(
  preferences: NotificationPreferences
): Promise<NotificationPreferences> {
  try {
    await apiClient.patch("/notifications/preferences", preferences);
    return preferences;
  } catch {
    return preferences;
  }
}

export async function fetchRecentNotifications(): Promise<NotificationItem[]> {
  try {
    const response = await apiClient.get("/notifications/events?size=6");
    const mapped = mapNotifications(response.data);
    return mapped.length > 0 ? mapped : fallbackNotifications;
  } catch {
    return fallbackNotifications;
  }
}

function mapNotifications(payload: unknown): NotificationItem[] {
  if (!Array.isArray(payload)) {
    return [];
  }

  return payload
    .map((row) => {
      if (!row || typeof row !== "object") {
        return null;
      }
      const data = row as Record<string, unknown>;
      const notificationId = asString(data.notificationId, "");
      if (!notificationId) {
        return null;
      }

      return {
        notificationId,
        title: asString(data.title, "Notification"),
        message: asString(data.message, "No detail available"),
        occurredAt: asString(data.occurredAt, new Date().toISOString()),
        level: asString(data.level, "Info") === "Warning" ? "Warning" : "Info"
      } satisfies NotificationItem;
    })
    .filter((notification): notification is NotificationItem => notification !== null);
}

function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asBoolean(value: unknown, fallback: boolean): boolean {
  return typeof value === "boolean" ? value : fallback;
}
