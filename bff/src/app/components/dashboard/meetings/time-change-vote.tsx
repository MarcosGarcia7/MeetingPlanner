'use client';

import type {TimeChangeProposalResponse, TimeChangeVoteRequest} from '@/lib/types/meeting';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import ScheduleConflictList from './schedule-conflict-list';
import styles from './meetings.module.css';

interface TimeChangeVoteProps {
  proposal: TimeChangeProposalResponse;
  timeZone?: string;
}

// Approve/Decline for a pending time change the viewer still has to vote on. Used by the meeting banner,
// the dashboard's vote card, and the Inbox. If the new time would double-book the viewer, Approve is
// locked (the API refuses it too) until the clash is gone: they can decline, leave this meeting, or leave
// (or, if they created it, delete) each clashing meeting. Every action refreshes the page, so the proposal
// comes back with fresh conflicts and Approve unlocks once there are none.
export default function TimeChangeVote({proposal, timeZone}: TimeChangeVoteProps) {
  const {run, busy, errorFor} = usePostAction();
  const key = `vote-${proposal.id}`;
  const error = errorFor(key);
  const voteUrl = `/api/meetings/${proposal.meetingId}/time-change/${proposal.id}/vote`;
  const blocked = proposal.conflicts.length > 0;
  const title = proposal.meetingTitle ?? 'this meeting';

  function vote(approve: boolean) {
    const body: TimeChangeVoteRequest = {approve};
    return run(key, voteUrl, body);
  }

  const leave = (meetingId: number) => run(key, `/api/meetings/${meetingId}/participants/me`, undefined, 'DELETE');
  const remove = (meetingId: number) => run(key, `/api/meetings/${meetingId}`, undefined, 'DELETE');

  return (
    <div className={styles.decision}>
      {blocked && (
        <ScheduleConflictList message='The new time would double-book you. Decline it, or clear the clash to approve:'
                              conflicts={proposal.conflicts} timeZone={timeZone} busy={busy}
                              onLeave={leave} onDelete={remove}/>
      )}
      {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
      <div className={dashboardStyles.actions}>
        <button type='button' className={dashboardStyles.button} disabled={busy || blocked}
                title={blocked ? 'Clear the clash on your schedule first' : undefined}
                onClick={() => vote(true)}>
          Approve
        </button>
        <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                onClick={() => vote(false)}>
          Decline
        </button>
        {blocked && !proposal.meetingCreatedByViewer && (
          <ConfirmButton label={`Leave ${title}`} confirmLabel={`Leave “${title}”`}
                         disabled={busy} onConfirm={() => leave(proposal.meetingId)}/>
        )}
      </div>
    </div>
  );
}
