'use client';

import type {ScheduleConflict} from '@/lib/types/meeting';
import {formatMeetingTime} from '@/app/components/dashboard/home/format';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

interface ScheduleConflictListProps {
  // Why the list is shown, e.g. "Accepting would double-book you. …".
  message: string;
  conflicts: ScheduleConflict[];
  timeZone?: string;
  busy: boolean;
  onLeave: (meetingId: number) => void;
  onDelete: (meetingId: number) => void;
}

// The viewer's meetings that a time change or an invite would clash with, each with a way to clear it:
// Leave for meetings they joined, Delete for ones they created (creators can't leave their own meetings).
// Shared by TimeChangeVote and InviteResponseActions.
export default function ScheduleConflictList({message, conflicts, timeZone, busy, onLeave, onDelete}:
                                             ScheduleConflictListProps) {
  return (
    <div className={styles.notes}>
      <span className={dashboardStyles.error}>{message}</span>
      <ul className={styles.conflictList}>
        {conflicts.map((conflict) => {
          const {day, time} = formatMeetingTime(conflict.startTime, conflict.endTime, timeZone);
          return (
            <li key={conflict.meetingId} className={styles.conflictRow}>
              <span className={dashboardStyles.meta}>“{conflict.title}” · {day}, {time}</span>
              <div className={dashboardStyles.actions}>
                {conflict.createdByViewer ? (
                  <ConfirmButton label='Delete meeting' confirmLabel={`Delete “${conflict.title}”`}
                                 disabled={busy} onConfirm={() => onDelete(conflict.meetingId)}/>
                ) : (
                  <ConfirmButton label='Leave meeting' confirmLabel={`Leave “${conflict.title}”`}
                                 disabled={busy} onConfirm={() => onLeave(conflict.meetingId)}/>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
