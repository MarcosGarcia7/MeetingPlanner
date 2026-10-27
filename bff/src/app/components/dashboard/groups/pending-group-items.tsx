'use client';

import type {InviteResponse} from '@/lib/types/invite';
import type {JoinRequestResponse} from '@/lib/types/request';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface PendingGroupItemsProps {
  invites: InviteResponse[];
  requests: JoinRequestResponse[];
}

// Group invites to answer and join requests still awaiting an admin. Rendered only when there are some.
export default function PendingGroupItems({invites, requests}: PendingGroupItemsProps) {
  const {run, busy, errorFor} = usePostAction();

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Invites & requests</h2>
      </div>
      <ul className={styles.groupList}>
        {invites.map((invite) => {
          const key = `invite-${invite.id}`;
          const error = errorFor(key);
          return (
            <li key={key} className={styles.groupCard}>
              <div className={styles.groupBody}>
                <span className={styles.groupName}>{invite.groupName}</span>
                <span className={dashboardStyles.meta}>@{invite.senderUsername} invited you</span>
                {error && <span className={dashboardStyles.error}>{error}</span>}
              </div>
              <div className={dashboardStyles.actions}>
                <button type='button' className={dashboardStyles.button} disabled={busy}
                        onClick={() => run(key, `/api/invites/group/${invite.id}/accept`)}>
                  Accept
                </button>
                <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                        onClick={() => run(key, `/api/invites/group/${invite.id}/decline`)}>
                  Decline
                </button>
              </div>
            </li>
          );
        })}
        {requests.map((request) => {
          const key = `request-${request.id}`;
          const error = errorFor(key);
          return (
            <li key={key} className={styles.groupCard}>
              <div className={styles.groupBody}>
                <span className={styles.groupName}>{request.groupName}</span>
                <span className={dashboardStyles.meta}>Waiting for an admin to approve your request</span>
                {error && <span className={dashboardStyles.error}>{error}</span>}
              </div>
              <div className={dashboardStyles.actions}>
                <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                        onClick={() => run(key, `/api/requests/group/${request.id}/cancel`)}>
                  Cancel request
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
