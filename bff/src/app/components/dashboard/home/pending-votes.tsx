'use client';

import type {TimeChangeProposalResponse} from '@/lib/types/meeting';
import {formatMeetingTime} from './format';
import TimeChangeVote from '@/app/components/dashboard/meetings/time-change-vote';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './home.module.css';

interface PendingVotesProps {
  proposals: TimeChangeProposalResponse[] | null;
  timeZone?: string;
}

export default function PendingVotes({proposals, timeZone}: PendingVotesProps) {
  // Nothing to vote on: keep the dashboard uncluttered rather than showing an empty card.
  if (proposals !== null && proposals.length === 0) return null;

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Time changes to vote on</h2>
      </div>
      {proposals === null ? (
        <p className={dashboardStyles.error}>Couldn’t load pending time changes.</p>
      ) : (
        <ul className={styles.list}>
          {proposals.map((proposal) => {
            const {day, time} = formatMeetingTime(proposal.proposedStartTime, proposal.proposedEndTime, timeZone);
            return (
              <li key={proposal.id} className={styles.decisionItem}>
                <div className={styles.itemBody}>
                  <span className={styles.itemTitle}>{proposal.meetingTitle ?? 'Meeting'}</span>
                  <span className={dashboardStyles.meta}>
                    @{proposal.proposedByUsername} proposed {day}, {time}
                  </span>
                </div>
                <TimeChangeVote proposal={proposal} timeZone={timeZone}/>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
