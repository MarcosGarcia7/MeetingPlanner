'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import clsx from 'clsx';
import type {CursorPage} from '@/lib/types/api';
import type {NotificationResponse} from '@/lib/types/notification';
import {sendJson} from '@/lib/send-json';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import {formatDateTime} from '@/app/components/dashboard/home/format';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import {describeNotification} from './notification-text';
import styles from './inbox.module.css';

interface ActivityFeedProps {
  // First page from the server render, or null when that request failed.
  initial: CursorPage<NotificationResponse> | null;
  timeZone: string;
}

// Overview's notification feed. Opening a row marks it read; the sidebar badge follows via router.refresh().
export default function ActivityFeed({initial, timeZone}: ActivityFeedProps) {
  const router = useRouter();
  const feed = useCursorList('/api/users/me/notifications', initial, 'Couldn’t load your activity.');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const hasUnread = feed.items.some((n) => !n.read);

  function markLocallyRead(id: number | null) {
    feed.setItems((items) => items.map((n) => (id === null || n.id === id ? {...n, read: true} : n)));
  }

  async function markAllRead() {
    setBusy(true);
    setError(null);
    const result = await sendJson('/api/users/me/notifications/read-all', 'POST');
    setBusy(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    markLocallyRead(null);
    router.refresh();
  }

  async function open(notification: NotificationResponse, href: string | null) {
    if (!notification.read && notification.id !== null) {
      const result = await sendJson(`/api/users/me/notifications/${notification.id}/read`, 'POST');
      if (result.ok) {
        markLocallyRead(notification.id);
      }
    }
    if (href) {
      router.push(href);
    } else {
      router.refresh();
    }
  }

  return (
    <section className={dashboardStyles.card}>
      <div className={styles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Activity</h2>
        {hasUnread && (
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy} onClick={markAllRead}>
            Mark all read
          </button>
        )}
      </div>
      {error && <p className={dashboardStyles.error}>{error}</p>}
      {feed.loaded && feed.items.length === 0 ? (
        <p className={dashboardStyles.empty}>No activity yet.</p>
      ) : (
        <ul className={homeStyles.cardList}>
          {feed.items.map((notification, index) => {
            const {text, href} = describeNotification(notification);
            return (
              <li key={notification.id ?? `n-${index}`}
                  className={clsx(dashboardStyles.itemRow, styles.activity, {[styles.unread]: !notification.read})}>
                <button type='button' className={clsx(homeStyles.itemBody, styles.activityLink)}
                        onClick={() => open(notification, href)}>
                  <span className={styles.activityText}>{text}</span>
                  {notification.createdAt && (
                    <span className={dashboardStyles.meta}>{formatDateTime(notification.createdAt, timeZone)}</span>
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      )}
      {feed.error && <p className={dashboardStyles.error}>{feed.error}</p>}
      {feed.hasMore && (
        <div className={styles.loadMore}>
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={feed.loading}
                  onClick={feed.loadMore}>
            {feed.loading ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
    </section>
  );
}
