'use client';

import {useState} from 'react';
import type {CursorPage} from '@/lib/types/api';
import type {MeetingResponse, MeetingSummaryResponse} from '@/lib/types/meeting';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import MeetingListItem from '@/app/components/dashboard/home/meeting-list-item';
import MeetingForm, {type InviteOutcome} from './meeting-form';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import styles from './groups.module.css';

interface GroupMeetingsProps {
  groupId: number;
  groupName: string | null;
  initial: CursorPage<MeetingSummaryResponse> | null;
  timeZone: string;
  memberUsernames: string[];
  // Admins and the owner always can; members only when the group allows it.
  canCreate: boolean;
}

function toSummary(meeting: MeetingResponse, groupName: string | null): MeetingSummaryResponse {
  return {
    id: meeting.id!,
    groupId: meeting.groupId,
    groupName,
    creatorId: meeting.creatorId!,
    startTime: meeting.startTime!,
    endTime: meeting.endTime,
    location: meeting.location,
    title: meeting.title ?? '',
    isPrivate: meeting.isPrivate,
    participantCount: meeting.participants?.length ?? 1,
  };
}

// The group's meetings, latest start first (past ones included), and the form to schedule one.
export default function GroupMeetings({groupId, groupName, initial, timeZone, memberUsernames,
                                        canCreate}: GroupMeetingsProps) {
  const meetings = useCursorList(`/api/groups/${groupId}/meetings`, initial, 'Couldn’t load meetings.');
  const [creating, setCreating] = useState(false);
  // Notes from the last meeting created here: the API's warnings and how its invites went.
  const [notes, setNotes] = useState<string[]>([]);
  const [failures, setFailures] = useState<string[]>([]);

  function onCreated(meeting: MeetingResponse, invites: InviteOutcome | null) {
    setCreating(false);
    const created = invites && invites.invited > 0
      ? `Meeting scheduled. Invited ${invites.invited} ${invites.invited === 1 ? 'person' : 'people'}.`
      : 'Meeting scheduled.';
    setNotes([created, ...(meeting.warnings ?? []), ...(invites?.warnings ?? [])]);
    setFailures(invites?.failed.length ? ['Some invites weren’t sent:', ...invites.failed] : []);
    const summary = toSummary(meeting, groupName);
    meetings.setItems((current) => {
      const index = current.findIndex((m) => Date.parse(m.startTime) < Date.parse(summary.startTime));
      // Past the loaded range: it shows up when the page it belongs to loads.
      if (index === -1) {
        return meetings.hasMore ? current : [...current, summary];
      }
      return [...current.slice(0, index), summary, ...current.slice(index)];
    });
  }

  return (
    <>
      {creating && (
        <section className={dashboardStyles.card}>
          <div className={dashboardStyles.cardHeader}>
            <h2 className={dashboardStyles.cardTitle}>New meeting</h2>
          </div>
          <MeetingForm groupId={groupId} timeZone={timeZone} memberUsernames={memberUsernames} onCreated={onCreated}
                       onCancel={() => setCreating(false)}/>
        </section>
      )}
      <section className={dashboardStyles.card}>
        <div className={styles.listHeader}>
          <h2 className={dashboardStyles.cardTitle}>Meetings</h2>
          {canCreate && !creating && (
            <button type='button' className={dashboardStyles.button} onClick={() => {
              setNotes([]);
              setFailures([]);
              setCreating(true);
            }}>
              New meeting
            </button>
          )}
        </div>
        {(notes.length > 0 || failures.length > 0) && (
          <div className={styles.notes} role='status'>
            {notes.map((note) => <p key={note} className={dashboardStyles.meta}>{note}</p>)}
            {failures.map((failure) => <p key={failure} className={dashboardStyles.error}>{failure}</p>)}
          </div>
        )}
        {meetings.loaded && meetings.items.length === 0 ? (
          <p className={dashboardStyles.empty}>No meetings in this group yet.</p>
        ) : (
          <ul className={homeStyles.cardList}>
            {meetings.items.map((meeting) => (
              <MeetingListItem key={meeting.id} meeting={meeting} timeZone={timeZone} showGroup={false}/>
            ))}
          </ul>
        )}
        {meetings.error && <p className={dashboardStyles.error}>{meetings.error}</p>}
        {meetings.hasMore && (
          <div className={styles.loadMore}>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={meetings.loading}
                    onClick={meetings.loadMore}>
              {meetings.loading ? 'Loading…' : 'Load more'}
            </button>
          </div>
        )}
      </section>
    </>
  );
}
