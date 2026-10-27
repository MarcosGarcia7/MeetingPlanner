'use client';

import type {MeetingResponse} from '@/lib/types/meeting';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

interface ParticipantListProps {
  meeting: MeetingResponse;
  viewerId: number;
}

// Who's coming: participants (the organizer first), then people who joined by an emailed link. Managers can
// remove anyone but the organizer.
export default function ParticipantList({meeting, viewerId}: ParticipantListProps) {
  const {run, busy, errorFor} = usePostAction();
  const participants = [...(meeting.participants ?? [])]
    .sort((a, b) => Number(b.userId === meeting.creatorId) - Number(a.userId === meeting.creatorId));
  const external = meeting.externalParticipantNames ?? [];

  return (
    <section className={dashboardStyles.card} aria-labelledby='participants-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='participants-title' className={dashboardStyles.cardTitle}>
          {participants.length + external.length} {participants.length + external.length === 1 ? 'person' : 'people'}
        </h2>
      </div>
      <ul className={styles.people}>
        {participants.map((participant) => {
          const key = `participant-${participant.userId}`;
          const error = errorFor(key);
          const isOrganizer = participant.userId === meeting.creatorId;
          return (
            <li key={key} className={styles.person}>
              <span className={styles.personName}>
                @{participant.username}
                {isOrganizer && <span className={styles.badge}>Organizer</span>}
                {participant.userId === viewerId && <span className={dashboardStyles.meta}>(you)</span>}
              </span>
              {error && <span className={dashboardStyles.error}>{error}</span>}
              {meeting.canManage && !isOrganizer && participant.userId !== viewerId && (
                <div className={dashboardStyles.actions}>
                  <ConfirmButton label='Remove' confirmLabel='Remove' disabled={busy}
                                 onConfirm={() => run(key, `/api/meetings/${meeting.id}/participants/${participant.userId}`,
                                   undefined, 'DELETE')}/>
                </div>
              )}
            </li>
          );
        })}
        {external.map((name) => (
          <li key={`external-${name}`} className={styles.person}>
            <span className={styles.personName}>
              {name}
              <span className={dashboardStyles.meta}>(joined by email link)</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
