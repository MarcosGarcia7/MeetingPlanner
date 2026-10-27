'use client';

import type {InviteResponse} from '@/lib/types/invite';
import type {JoinRequestResponse} from '@/lib/types/request';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import styles from './schedule.module.css';

interface PendingMeetingItemsProps {
  invites: InviteResponse[];
  requests: JoinRequestResponse[];
}

// Meeting invites to answer and join requests still awaiting the creator. Rendered only when there are some.
export default function PendingMeetingItems({invites, requests}: PendingMeetingItemsProps) {
  const {run, busy, errorFor} = usePostAction();

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Invites & requests</h2>
      </div>
      <ul className={homeStyles.cardList}>
        {invites.map((invite) => {
          const key = `invite-${invite.id}`;
          const error = errorFor(key);
          return (
            <li key={key} className={styles.itemRow}>
              <div className={homeStyles.itemBody}>
                <span className={homeStyles.itemTitle}>{invite.meetingTitle}</span>
                <span className={dashboardStyles.meta}>@{invite.senderUsername} invited you</span>
                {invite.warnings.map((warning) => <span key={warning} className={dashboardStyles.meta}>{warning}</span>)}
                {error && <span className={dashboardStyles.error}>{error}</span>}
              </div>
              <div className={dashboardStyles.actions}>
                <button type='button' className={dashboardStyles.button} disabled={busy}
                        onClick={() => run(key, `/api/invites/meeting/${invite.id}/accept`)}>
                  Accept
                </button>
                <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                        onClick={() => run(key, `/api/invites/meeting/${invite.id}/decline`)}>
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
            <li key={key} className={styles.itemRow}>
              <div className={homeStyles.itemBody}>
                <span className={homeStyles.itemTitle}>{request.meetingTitle}</span>
                <span className={dashboardStyles.meta}>Waiting for the organizer to approve your request</span>
                {error && <span className={dashboardStyles.error}>{error}</span>}
              </div>
              <div className={dashboardStyles.actions}>
                <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                        onClick={() => run(key, `/api/requests/meeting/${request.id}/cancel`)}>
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
