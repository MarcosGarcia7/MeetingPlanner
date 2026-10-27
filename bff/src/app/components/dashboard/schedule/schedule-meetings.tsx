'use client';

import {useState} from 'react';
import Link from 'next/link';
import clsx from 'clsx';
import type {CursorPage} from '@/lib/types/api';
import type {GroupResponse} from '@/lib/types/group';
import type {MeetingResponse, MeetingSummaryResponse} from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import MeetingListItem from '@/app/components/dashboard/home/meeting-list-item';
import MeetingForm, {type GroupOption, type InviteOutcome} from '@/app/components/dashboard/meetings/meeting-form';
import {creationNotes, toSummary} from '@/app/components/dashboard/meetings/summary';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import {type MeetingsView, VIEWS} from './meetings-views';
import styles from './schedule.module.css';

interface ScheduleMeetingsProps {
  view: MeetingsView;
  // First page from the server render, or null when that request failed.
  initial: CursorPage<MeetingSummaryResponse> | null;
  timeZone: string;
  // Groups the viewer may schedule in.
  groups: GroupOption[];
  friendUsernames: string[];
  viewerId: number;
  // Open the form on load (linked as ?new=1).
  creating: boolean;
}

// The Meetings tab: every meeting the user is in, filtered to Upcoming (soonest first), Past (latest
// first), or the ones they created (latest first), plus the form to schedule one.
export default function ScheduleMeetings({view, initial, timeZone, groups, friendUsernames, viewerId,
                                          creating: initiallyCreating}: ScheduleMeetingsProps) {
  const current = VIEWS.find((v) => v.view === view)!;
  const meetings = useCursorList(current.url, initial, 'Couldn’t load your meetings.');
  const [creating, setCreating] = useState(initiallyCreating);
  const [notes, setNotes] = useState<string[]>([]);
  const [failures, setFailures] = useState<string[]>([]);
  // Invite suggestions follow the group picked in the form: its other members, fetched once per group.
  // Friends are suggested with no group, and while (or if) a group's members can't be loaded.
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [membersByGroup, setMembersByGroup] = useState<Record<number, string[]>>({});
  const suggestions = selectedGroup === null ? friendUsernames : membersByGroup[selectedGroup] ?? friendUsernames;

  async function onGroupChange(groupId: number | null) {
    setSelectedGroup(groupId);
    if (groupId === null || membersByGroup[groupId]) {
      return;
    }
    const result = await sendJson<GroupResponse>(`/api/groups/${groupId}`);
    if (result.ok) {
      const usernames = (result.data.members ?? []).flatMap((member) =>
        member.userId !== viewerId && member.username ? [member.username] : []);
      setMembersByGroup((current) => ({...current, [groupId]: usernames}));
    }
  }

  function openForm(open: boolean) {
    setCreating(open);
    // The form starts at "No group" each time it opens.
    setSelectedGroup(null);
  }

  function onCreated(meeting: MeetingResponse, invites: InviteOutcome | null) {
    openForm(false);
    const outcome = creationNotes(meeting, invites);
    setNotes(outcome.notes);
    setFailures(outcome.failures);
    // A new meeting is never past; elsewhere it goes where its start time sorts in this view's order.
    if (view === 'past') {
      return;
    }
    const summary = toSummary(meeting);
    const start = Date.parse(summary.startTime);
    const comesBefore = (m: MeetingSummaryResponse) =>
      view === 'upcoming' ? Date.parse(m.startTime) > start : Date.parse(m.startTime) < start;
    meetings.setItems((items) => {
      const index = items.findIndex(comesBefore);
      // Past the loaded range: it shows up when the page it belongs to loads.
      if (index === -1) {
        return meetings.hasMore ? items : [...items, summary];
      }
      return [...items.slice(0, index), summary, ...items.slice(index)];
    });
  }

  return (
    <>
      {creating && (
        <section className={dashboardStyles.card}>
          <div className={dashboardStyles.cardHeader}>
            <h2 className={dashboardStyles.cardTitle}>New meeting</h2>
          </div>
          <MeetingForm groups={groups} timeZone={timeZone} suggestions={suggestions} onGroupChange={onGroupChange}
                       onCreated={onCreated} onCancel={() => openForm(false)}/>
        </section>
      )}
      <section className={dashboardStyles.card}>
        <div className={styles.cardHeader}>
          <nav className={dashboardStyles.filters} aria-label='Which meetings'>
            {VIEWS.map((v) => (
              <Link key={v.view} href={v.view === 'upcoming' ? '/dashboard/schedule/meetings'
                : `/dashboard/schedule/meetings?view=${v.view}`}
                    aria-current={v.view === view ? 'page' : undefined}
                    className={clsx(dashboardStyles.filter, {[dashboardStyles.currentFilter]: v.view === view})}>
                {v.label}
              </Link>
            ))}
          </nav>
          {!creating && (
            <button type='button' className={dashboardStyles.button} onClick={() => {
              setNotes([]);
              setFailures([]);
              openForm(true);
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
          <p className={dashboardStyles.empty}>{current.empty}</p>
        ) : (
          <ul className={homeStyles.cardList}>
            {meetings.items.map((meeting) => (
              <MeetingListItem key={meeting.id} meeting={meeting} timeZone={timeZone}/>
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
