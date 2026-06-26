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

export async function fetchNotificationPreferences(): Promise<NotificationPreferences> {
  const response = await apiClient.get("/notifications/preferences");
  const data = response.data as Record<string, unknown>;
  return {
    pushEnabled: asBoolean(data.pushEnabled, false),
    emailEnabled: asBoolean(data.emailEnabled, false),
    smsEnabled: asBoolean(data.smsEnabled, false),
    marketingEnabled: asBoolean(data.marketingEnabled, false)
  };
}

export async function updateNotificationPreferences(
  preferences: NotificationPreferences
): Promise<NotificationPreferences> {
  await apiClient.patch("/notifications/preferences", preferences);
  return preferences;
}

export async function fetchRecentNotifications(): Promise<NotificationItem[]> {
  const response = await apiClient.get("/notifications/events?size=6");
  return mapNotifications(response.data);
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
