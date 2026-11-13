'use client';

import type {InviteResponse} from '@/lib/types/invite';
import type {JoinRequestResponse} from '@/lib/types/request';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import {inviteDetails} from '@/app/components/dashboard/inbox/items';
import InviteResponseActions from '@/app/components/dashboard/meetings/invite-response';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';

interface PendingMeetingItemsProps {
  invites: InviteResponse[];
  requests: JoinRequestResponse[];
  // Profile time zone, so server and client render the same meeting times.
  timeZone: string;
}

// Meeting invites to answer (with when and where, and any clash with the viewer's schedule) and join
// requests still awaiting the creator. Rendered only when there are some.
export default function PendingMeetingItems({invites, requests, timeZone}: PendingMeetingItemsProps) {
  const {run, busy, errorFor} = usePostAction();

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Invites & requests</h2>
      </div>
      <ul className={homeStyles.cardList}>
        {invites.map((invite) => (
          <li key={`invite-${invite.id}`} className={dashboardStyles.itemRow}>
            <div className={homeStyles.itemBody}>
              <span className={homeStyles.itemTitle}>{invite.meetingTitle}</span>
              <span className={dashboardStyles.meta}>@{invite.senderUsername} invited you</span>
              {inviteDetails(invite, timeZone).map((detail) => (
                <span key={detail} className={dashboardStyles.meta}>{detail}</span>
              ))}
            </div>
            <InviteResponseActions inviteId={invite.id} conflicts={invite.conflicts} timeZone={timeZone}/>
          </li>
        ))}
        {requests.map((request) => {
          const key = `request-${request.id}`;
          const error = errorFor(key);
          return (
            <li key={key} className={dashboardStyles.itemRow}>
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
