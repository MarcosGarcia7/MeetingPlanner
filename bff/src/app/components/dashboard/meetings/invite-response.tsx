'use client';

import type {ScheduleConflict} from '@/lib/types/meeting';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import ScheduleConflictList from './schedule-conflict-list';
import styles from './meetings.module.css';

interface InviteResponseActionsProps {
  inviteId: number;
  // The viewer's meetings this one overlaps (InviteResponse.conflicts / MeetingResponse.viewerConflicts).
  conflicts: ScheduleConflict[];
  timeZone?: string;
  acceptLabel?: string;
}

// Accept/Decline for a pending meeting invite: the invite counterpart of TimeChangeVote. Used by the
// Schedule card, the Inbox, the home "Needs your attention" card, and the meeting page header. If the
// meeting would double-book the viewer, Accept is locked (the API refuses it too) until the clash is
// gone: they can decline, or leave (or, if they created it, delete) each clashing meeting. Every action
// refreshes the page, so the invite comes back with fresh conflicts and Accept unlocks once there are none.
export default function InviteResponseActions({inviteId, conflicts, timeZone, acceptLabel = 'Accept'}:
                                              InviteResponseActionsProps) {
  const {run, busy, errorFor} = usePostAction();
  const key = `invite-${inviteId}`;
  const error = errorFor(key);
  const base = `/api/invites/meeting/${inviteId}`;
  const blocked = conflicts.length > 0;

  const leave = (meetingId: number) => run(key, `/api/meetings/${meetingId}/participants/me`, undefined, 'DELETE');
  const remove = (meetingId: number) => run(key, `/api/meetings/${meetingId}`, undefined, 'DELETE');

  return (
    <div className={styles.decision}>
      {blocked && (
        <ScheduleConflictList message='Accepting would double-book you. Decline it, or clear the clash to accept:'
                              conflicts={conflicts} timeZone={timeZone} busy={busy}
                              onLeave={leave} onDelete={remove}/>
      )}
      {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
      <div className={dashboardStyles.actions}>
        <button type='button' className={dashboardStyles.button} disabled={busy || blocked}
                title={blocked ? 'Clear the clash on your schedule first' : undefined}
                onClick={() => run(key, `${base}/accept`)}>
          {acceptLabel}
        </button>
        <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                onClick={() => run(key, `${base}/decline`)}>
          Decline
        </button>
      </div>
    </div>
  );
}
