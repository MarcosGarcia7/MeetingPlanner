'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {LockClosedIcon} from '@heroicons/react/24/solid';
import type {MeetingResponse} from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import {formatLongDate, formatMeetingTime} from '@/app/components/dashboard/home/format';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

interface MeetingHeaderProps {
  meeting: MeetingResponse;
  viewerId: number;
  timeZone: string;
}

// Title, badges, and when it is, plus Leave for participants other than the creator (who deletes the
// meeting from Manage instead).
export default function MeetingHeader({meeting, viewerId, timeZone}: MeetingHeaderProps) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isCreator = meeting.creatorId === viewerId;
  const isParticipant = meeting.participants?.some((p) => p.userId === viewerId) ?? false;
  const {time} = formatMeetingTime(meeting.startTime!, meeting.endTime, timeZone);

  async function leave() {
    setBusy(true);
    setError(null);
    const result = await sendJson(`/api/meetings/${meeting.id}/participants/me`, 'DELETE');
    if (!result.ok) {
      setBusy(false);
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    router.push('/dashboard/schedule/meetings');
    router.refresh();
  }

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
        {isParticipant && !isCreator && (
          <div className={dashboardStyles.actions}>
            <ConfirmButton label='Leave meeting' confirmLabel='Leave' disabled={busy} onConfirm={leave}/>
          </div>
        )}
      </div>
    </section>
  );
}
