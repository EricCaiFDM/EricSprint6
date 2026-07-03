import { apiClient } from "./api";

export type NotificationPreferences = {
  depositAlertsEnabled: boolean;
  withdrawalAlertsEnabled: boolean;
  transferAlertsEnabled: boolean;
  statementAlertsEnabled: boolean;
  offersEnabled: boolean;
};

export type NotificationItem = {
  notificationId: string;
  title: string;
  message: string;
  occurredAt: string;
  level: "Info" | "Warning";
};

export type NotificationTopic = "deposit" | "withdrawal" | "transfer" | "statement" | "offers" | "other";

export async function fetchNotificationPreferences(): Promise<NotificationPreferences> {
  const response = await apiClient.get("/notifications/preferences");
  const data = response.data as Record<string, unknown>;
  return {
    depositAlertsEnabled: asBoolean(data.depositAlertsEnabled, true),
    withdrawalAlertsEnabled: asBoolean(data.withdrawalAlertsEnabled, true),
    transferAlertsEnabled: asBoolean(data.transferAlertsEnabled, true),
    statementAlertsEnabled: asBoolean(data.statementAlertsEnabled, true),
    offersEnabled: asBoolean(data.offersEnabled, false)
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

export function resolveNotificationTopic(notification: Pick<NotificationItem, "title" | "message">): NotificationTopic {
  const normalized = `${notification.title} ${notification.message}`.toUpperCase();

  if (normalized.includes("DEPOSIT")) {
    return "deposit";
  }

  if (normalized.includes("WITHDRAW")) {
    return "withdrawal";
  }

  if (normalized.includes("TRANSFER") || normalized.includes("STANDING ORDER")) {
    return "transfer";
  }

  if (normalized.includes("STATEMENT")) {
    return "statement";
  }

  if (
    normalized.includes("PROMOTION")
    || normalized.includes("PROMO")
    || normalized.includes("OFFER")
    || normalized.includes("MARKETING")
  ) {
    return "offers";
  }

  return "other";
}

export function isNotificationEnabledByPreferences(
  notification: Pick<NotificationItem, "title" | "message">,
  preferences: NotificationPreferences
): boolean {
  switch (resolveNotificationTopic(notification)) {
    case "deposit":
      return preferences.depositAlertsEnabled;
    case "withdrawal":
      return preferences.withdrawalAlertsEnabled;
    case "transfer":
      return preferences.transferAlertsEnabled;
    case "statement":
      return preferences.statementAlertsEnabled;
    case "offers":
      return preferences.offersEnabled;
    default:
      return true;
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
