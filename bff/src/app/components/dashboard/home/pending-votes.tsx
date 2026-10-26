'use client';

import type {TimeChangeProposalResponse, TimeChangeVoteRequest} from '@/lib/types/meeting';
import {formatMeetingTime} from './format';
import {usePostAction} from './use-post-action';
import styles from './home.module.css';

interface PendingVotesProps {
  proposals: TimeChangeProposalResponse[] | null;
  timeZone?: string;
}

export default function PendingVotes({proposals, timeZone}: PendingVotesProps) {
  const {run, busy, errorFor} = usePostAction();

  // Nothing to vote on: keep the dashboard uncluttered rather than showing an empty card.
  if (proposals !== null && proposals.length === 0) return null;

  function vote(proposal: TimeChangeProposalResponse, approve: boolean) {
    const body: TimeChangeVoteRequest = {approve};
    return run(
      `vote-${proposal.id}`,
      `/api/meetings/${proposal.meetingId}/time-change/${proposal.id}/vote`,
      body
    );
  }

  return (
    <section className={styles.card}>
      <div className={styles.cardHeader}>
        <h2 className={styles.cardTitle}>Time changes to vote on</h2>
      </div>
      {proposals === null ? (
        <p className={styles.error}>Couldn’t load pending time changes.</p>
      ) : (
        <ul className={styles.list}>
          {proposals.map((proposal) => {
            const {day, time} = formatMeetingTime(proposal.proposedStartTime, proposal.proposedEndTime, timeZone);
            const error = errorFor(`vote-${proposal.id}`);
            const conflicts = proposal.conflicts.length;
            return (
              <li key={proposal.id} className={styles.item}>
                <div className={styles.itemBody}>
                  <span className={styles.itemTitle}>{proposal.meetingTitle ?? 'Meeting'}</span>
                  <span className={styles.meta}>
                    @{proposal.proposedByUsername} proposed {day}, {time}
                  </span>
                  {conflicts > 0 && (
                    <span className={styles.error}>
                      Conflicts with {conflicts} of your meetings
                    </span>
                  )}
                  {error && <span className={styles.error}>{error}</span>}
                </div>
                <div className={styles.actions}>
                  <button type='button' className={styles.button} disabled={busy}
                          onClick={() => vote(proposal, true)}>
                    Approve
                  </button>
                  <button type='button' className={styles.buttonSecondary} disabled={busy}
                          onClick={() => vote(proposal, false)}>
                    Decline
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
