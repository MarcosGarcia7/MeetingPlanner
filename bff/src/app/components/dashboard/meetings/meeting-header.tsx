'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {LockClosedIcon} from '@heroicons/react/24/solid';
import type {MeetingResponse} from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import {formatLongDate, formatMeetingTime} from '@/app/components/dashboard/home/format';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

interface MeetingHeaderProps {
  meeting: MeetingResponse;
  viewerId: number;
  timeZone: string;
}

// Title, badges, and when it is, plus the viewer's action: Leave for participants other than the creator
// (who deletes the meeting from Manage instead), Accept/Decline for a pending invite, Cancel for a pending
// join request, or Request to join when the meeting is open to requests.
export default function MeetingHeader({meeting, viewerId, timeZone}: MeetingHeaderProps) {
  const router = useRouter();
  const [leaving, setLeaving] = useState(false);
  const [leaveError, setLeaveError] = useState<string | null>(null);
  const {run, busy: acting, errorFor} = usePostAction();
  const isCreator = meeting.creatorId === viewerId;
  const busy = leaving || acting;
  const error = leaveError ?? errorFor('meeting');
  const {time} = formatMeetingTime(meeting.startTime!, meeting.endTime, timeZone);

  async function leave() {
    setLeaving(true);
    setLeaveError(null);
    const result = await sendJson(`/api/meetings/${meeting.id}/participants/me`, 'DELETE');
    if (!result.ok) {
      setLeaving(false);
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setLeaveError(result.message);
      return;
    }
    router.push('/dashboard/schedule/meetings');
    router.refresh();
  }

  function act(url: string) {
    setLeaveError(null);
    void run('meeting', url);
  }

  function renderActions() {
    switch (meeting.viewerStatus) {
      case 'PARTICIPANT':
        return isCreator ? null : (
          <ConfirmButton label='Leave meeting' confirmLabel='Leave' disabled={busy} onConfirm={leave}/>
        );
      case 'INVITED':
        return (
          <>
            <button type='button' className={dashboardStyles.button} disabled={busy}
                    onClick={() => act(`/api/invites/meeting/${meeting.pendingInviteId}/accept`)}>
              Accept invite
            </button>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                    onClick={() => act(`/api/invites/meeting/${meeting.pendingInviteId}/decline`)}>
              Decline
            </button>
          </>
        );
      case 'REQUESTED':
        return (
          <>
            <span className={dashboardStyles.meta}>Requested</span>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                    onClick={() => act(`/api/requests/meeting/${meeting.pendingRequestId}/cancel`)}>
              Cancel request
            </button>
          </>
        );
      case 'CAN_REQUEST':
        return (
          <button type='button' className={dashboardStyles.button} disabled={busy}
                  onClick={() => act(`/api/meetings/${meeting.id}/join-requests`)}>
            Request to join
          </button>
        );
      default:
        return null;
    }
  }

  const actions = renderActions();

  return (
    <section className={dashboardStyles.card}>
      <div className={styles.header}>
        <div className={styles.headerBody}>
          <h1 className={styles.title}>
            {meeting.title}
            {isCreator && <span className={styles.badge}>Organizer</span>}
            {meeting.isPrivate && (
              <span className={styles.badge}>
                <LockClosedIcon className={styles.badgeIcon} aria-hidden/>
                Private
              </span>
            )}
          </h1>
          <span className={styles.when}>
            <time dateTime={meeting.startTime!}>{formatLongDate(meeting.startTime!, timeZone)}</time> · {time}
          </span>
          {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
        </div>
        {actions && <div className={dashboardStyles.actions}>{actions}</div>}
      </div>
    </section>
  );
}
