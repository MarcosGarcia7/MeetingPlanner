'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CursorPage} from '@/lib/types/api';
import type {JoinRequestResponse} from '@/lib/types/request';
import {sendJson} from '@/lib/send-json';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import {formatDateTime} from '@/app/components/dashboard/home/format';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface JoinRequestListProps {
  groupId: number;
  initial: CursorPage<JoinRequestResponse> | null;
  timeZone: string;
}

// Pending requests to join, newest first, with approve and decline.
export default function JoinRequestList({groupId, initial, timeZone}: JoinRequestListProps) {
  const router = useRouter();
  const requests = useCursorList(`/api/groups/${groupId}/join-requests?status=PENDING`, initial,
    'Couldn’t load join requests.');
  const [pending, setPending] = useState<number | null>(null);
  const [error, setError] = useState<{ id: number; message: string } | null>(null);

  async function respond(id: number, action: 'approve' | 'decline') {
    setPending(id);
    setError(null);
    const result = await sendJson(`/api/requests/group/${id}/${action}`, 'POST');
    setPending(null);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError({id, message: result.message});
      return;
    }
    requests.setItems((current) => current.filter((r) => r.id !== id));
    if (action === 'approve') {
      // The member count and the Members tab change.
      router.refresh();
    }
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='join-requests-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='join-requests-title' className={dashboardStyles.cardTitle}>Join requests</h2>
      </div>
      {requests.loaded && requests.items.length === 0 ? (
        <p className={dashboardStyles.empty}>No one is waiting to join.</p>
      ) : (
        <ul className={styles.groupList}>
          {requests.items.map((request) => (
            <li key={request.id} className={styles.groupCard}>
              <div className={styles.groupBody}>
                <span className={styles.groupName}>@{request.requesterUsername}</span>
                {request.timeSent && (
                  <span className={dashboardStyles.meta}>Asked {formatDateTime(request.timeSent, timeZone)}</span>
                )}
                {error?.id === request.id && <span className={dashboardStyles.error}>{error.message}</span>}
              </div>
              <div className={dashboardStyles.actions}>
                <button type='button' className={dashboardStyles.button} disabled={pending === request.id}
                        onClick={() => respond(request.id!, 'approve')}>
                  Approve
                </button>
                <button type='button' className={dashboardStyles.buttonSecondary} disabled={pending === request.id}
                        onClick={() => respond(request.id!, 'decline')}>
                  Decline
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {requests.error && <p className={dashboardStyles.error}>{requests.error}</p>}
      {requests.hasMore && (
        <div className={styles.loadMore}>
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={requests.loading}
                  onClick={requests.loadMore}>
            {requests.loading ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
    </section>
  );
}
