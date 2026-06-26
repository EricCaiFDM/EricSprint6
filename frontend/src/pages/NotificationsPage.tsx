import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  fetchNotificationPreferences,
  fetchRecentNotifications,
  updateNotificationPreferences,
  type NotificationPreferences
} from "../services/notifications";
import { formatDateTime } from "../utils/formatting";

export function NotificationsPage() {
  const preferencesQuery = useQuery({
    queryKey: ["notification-preferences"],
    queryFn: fetchNotificationPreferences
  });
  const feedQuery = useQuery({
    queryKey: ["notification-feed"],
    queryFn: fetchRecentNotifications
  });

  const [preferences, setPreferences] = useState<NotificationPreferences>({
    pushEnabled: false,
    emailEnabled: false,
    smsEnabled: false,
    marketingEnabled: false
  });
  const [feedback, setFeedback] = useState("Choose how you want to hear from us.");

  useEffect(() => {
    if (preferencesQuery.data) {
      setPreferences(preferencesQuery.data);
    }
  }, [preferencesQuery.data]);

  const saveMutation = useMutation({
    mutationFn: updateNotificationPreferences
  });

  const savePreferences = async () => {
    try {
      await saveMutation.mutateAsync(preferences);
      setFeedback("Preferences updated.");
    } catch (error) {
      setFeedback(`Unable to save preferences: ${(error as Error).message}`);
    }
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Notifications</h2>
          <p className="page-subtitle">Control how account alerts and service updates reach you.</p>
        </div>
      </header>

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Delivery preferences</h3>
          <div className="toggle-list">
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.pushEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, pushEnabled: event.target.checked })
                }
              />
              <span>Push notifications</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.emailEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, emailEnabled: event.target.checked })
                }
              />
              <span>Email alerts</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.smsEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, smsEnabled: event.target.checked })
                }
              />
              <span>SMS alerts</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.marketingEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, marketingEnabled: event.target.checked })
                }
              />
              <span>Product updates and offers</span>
            </label>
          </div>
          <div className="actions">
            <button type="button" onClick={savePreferences} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? "Saving..." : "Save preferences"}
            </button>
          </div>
          <p className="hint-text">{feedback}</p>
        </article>

        <article className="surface-card">
          <h3>Recent alerts</h3>
          <ul className="stack-list">
            {(feedQuery.data ?? []).map((notification) => (
              <li key={notification.notificationId} className="stack-list-item">
                <div>
                  <p className="item-title">{notification.title}</p>
                  <p className="item-meta">{notification.message}</p>
                </div>
                <div className="stack-list-meta">
                  <span className={notification.level === "Warning" ? "status-pill status-pill--warn" : "status-pill status-pill--ok"}>
                    {notification.level}
                  </span>
                  <p className="item-meta">{formatDateTime(notification.occurredAt)}</p>
                </div>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </section>
  );
}
