'use client';

import type {TimeChangeProposalResponse} from '@/lib/types/meeting';
import {formatLongDate, formatMeetingTime} from '@/app/components/dashboard/home/format';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import TimeChangeVote from './time-change-vote';
import styles from './meetings.module.css';

interface TimeChangeBannerProps {
  proposal: TimeChangeProposalResponse;
  viewerId: number;
  timeZone: string;
}

// A pending time change on this meeting. Participants who haven't voted can approve or decline (one
// decline rejects it; it applies once everyone approves) through TimeChangeVote, which also stops them
// approving into a clash on their schedule. Whoever proposed it can withdraw it.
export default function TimeChangeBanner({proposal, viewerId, timeZone}: TimeChangeBannerProps) {
  const {run, busy, errorFor} = usePostAction();
  const key = `proposal-${proposal.id}`;
  const error = errorFor(key);
  const base = `/api/meetings/${proposal.meetingId}/time-change/${proposal.id}`;
  const {time} = formatMeetingTime(proposal.proposedStartTime, proposal.proposedEndTime, timeZone);
  const proposedByMe = proposal.proposedById === viewerId;

  return (
    <section className={styles.banner} aria-labelledby='time-change-title'>
      <div className={styles.headerBody}>
        <h2 id='time-change-title' className={dashboardStyles.cardTitle}>Proposed new time</h2>
        <span className={styles.when}>
          {formatLongDate(proposal.proposedStartTime, timeZone)} · {time}
        </span>
        <span className={dashboardStyles.meta}>
          {proposedByMe ? 'You proposed this. It applies once everyone approves.'
            : `Proposed by @${proposal.proposedByUsername}.`}
          {proposal.myVote === 'APPROVED' && ' You approved it; waiting on the others.'}
        </span>
        {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
      </div>
      {proposedByMe && (
        <div className={dashboardStyles.actions}>
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                  onClick={() => run(key, `${base}/cancel`)}>
            Withdraw
          </button>
        </div>
      )}
      {proposal.myVote === 'PENDING' && <TimeChangeVote proposal={proposal} timeZone={timeZone}/>}
    </section>
  );
}
