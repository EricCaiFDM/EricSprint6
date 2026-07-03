import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  fetchNotificationPreferences,
  fetchRecentNotifications,
  isNotificationEnabledByPreferences,
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
    queryFn: fetchRecentNotifications,
    refetchInterval: 5000,
    refetchIntervalInBackground: true
  });

  const [preferences, setPreferences] = useState<NotificationPreferences>({
    depositAlertsEnabled: true,
    withdrawalAlertsEnabled: true,
    transferAlertsEnabled: true,
    statementAlertsEnabled: true,
    offersEnabled: false
  });
  const [feedback, setFeedback] = useState("Choose which account activity and updates you want to be notified about.");
  const [liveAlert, setLiveAlert] = useState<string | null>(null);
  const latestNotificationIdRef = useRef<string | null>(null);

  useEffect(() => {
    if (preferencesQuery.data) {
      setPreferences(preferencesQuery.data);
    }
  }, [preferencesQuery.data]);

  useEffect(() => {
    const latest = feedQuery.data?.[0];
    if (!latest) {
      return;
    }

    if (latestNotificationIdRef.current === null) {
      latestNotificationIdRef.current = latest.notificationId;
      return;
    }

    if (latest.notificationId !== latestNotificationIdRef.current) {
      latestNotificationIdRef.current = latest.notificationId;

      if (!isNotificationEnabledByPreferences(latest, preferences)) {
        return;
      }

      setLiveAlert(`New alert: ${latest.title}`);
    }
  }, [feedQuery.data, preferences]);

  const saveMutation = useMutation({
    mutationFn: updateNotificationPreferences
  });

  const savePreferences = async () => {
    try {
      await saveMutation.mutateAsync(preferences);
      setLiveAlert("Preferences have been updated.");
      setFeedback("Choose which account activity and updates you want to be notified about.");
    } catch (error) {
      setFeedback(`Unable to save preferences: ${(error as Error).message}`);
    }
  };

  return (
    <section className="bank-page">
      <header className="page-header">
        <div>
          <h2 className="page-title">Notifications</h2>
          <p className="page-subtitle">Select what types of account activity and updates you want to be notified about.</p>
        </div>
      </header>

      {liveAlert ? (
        <div className="in-page-alert" role="alert">
          <span>{liveAlert}</span>
          <button type="button" className="in-page-alert-dismiss" onClick={() => setLiveAlert(null)}>
            Dismiss
          </button>
        </div>
      ) : null}

      <section className="two-column-grid">
        <article className="surface-card">
          <h3>Notification topics</h3>
          <div className="toggle-list">
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.depositAlertsEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, depositAlertsEnabled: event.target.checked })
                }
              />
              <span>Deposit confirmations</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.withdrawalAlertsEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, withdrawalAlertsEnabled: event.target.checked })
                }
              />
              <span>Withdrawal confirmations</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.transferAlertsEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, transferAlertsEnabled: event.target.checked })
                }
              />
              <span>Transfer updates</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.statementAlertsEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, statementAlertsEnabled: event.target.checked })
                }
              />
              <span>Statement ready alerts</span>
            </label>
            <label className="toggle-item">
              <input
                type="checkbox"
                checked={preferences.offersEnabled}
                onChange={(event) =>
                  setPreferences({ ...preferences, offersEnabled: event.target.checked })
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
