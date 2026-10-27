'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {InviteBatchResponse, InviteCheckResponse} from '@/lib/types/invite';
import type {MeetingInviteBatchRequest} from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import InviteeInput, {toInviteRequest} from '@/app/components/dashboard/groups/invitee-input';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

interface MeetingInviteFormProps {
  meetingId: number;
  // A group meeting suggests the group's other members; otherwise friends. Current participants excluded.
  suggestions: string[];
}

function who(entry: { username: string | null; email: string | null }) {
  return entry.username ? `@${entry.username}` : entry.email;
}

// Invites more people. "Check schedules" asks the API which invitees the meeting would clash with (for
// people whose schedule the viewer can see) before anything is sent.
export default function MeetingInviteForm({meetingId, suggestions}: MeetingInviteFormProps) {
  const router = useRouter();
  const [invitees, setInvitees] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [notes, setNotes] = useState<string[]>([]);
  const [failures, setFailures] = useState<string[]>([]);

  function body(): MeetingInviteBatchRequest {
    return {recipients: invitees.map(toInviteRequest)};
  }

  async function check() {
    setBusy(true);
    setNotes([]);
    setFailures([]);
    const result = await sendJson<InviteCheckResponse>(`/api/meetings/${meetingId}/invites/check`, 'POST', body());
    setBusy(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setFailures([result.message]);
      return;
    }
    const warnings = (result.data.recipients ?? []).flatMap((recipient) =>
      (recipient.warnings ?? []).map((warning) => `${who(recipient)}: ${warning}`));
    setNotes(warnings.length > 0 ? warnings : ['No clashes found for the people whose schedules you can see.']);
  }

  async function send() {
    setBusy(true);
    setNotes([]);
    setFailures([]);
    const result = await sendJson<InviteBatchResponse>(`/api/meetings/${meetingId}/invites`, 'POST', body());
    setBusy(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setFailures([result.message]);
      return;
    }
    const invited = result.data.invited.length;
    setNotes([
      `Invited ${invited} ${invited === 1 ? 'person' : 'people'}.`,
      ...result.data.invited.flatMap((invite) => invite.warnings),
    ]);
    setFailures(result.data.failed.length
      ? ['Some invites weren’t sent:', ...result.data.failed.map((f) => `${who(f)}: ${f.reason ?? 'not invited'}`)]
      : []);
    setInvitees([]);
    router.refresh();
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='invite-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='invite-title' className={dashboardStyles.cardTitle}>Invite people</h2>
      </div>
      <div className={styles.stack}>
        <InviteeInput value={invitees} onChange={(value) => {
          setInvitees(value);
          setNotes([]);
          setFailures([]);
        }} suggestions={suggestions} disabled={busy} label='Usernames or emails'/>
        {(notes.length > 0 || failures.length > 0) && (
          <div className={styles.notes} role='status'>
            {notes.map((note) => <p key={note} className={dashboardStyles.meta}>{note}</p>)}
            {failures.map((failure) => <p key={failure} className={dashboardStyles.error}>{failure}</p>)}
          </div>
        )}
        <div className={styles.formFooter}>
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy || invitees.length === 0}
                  onClick={check}>
            Check schedules
          </button>
          <button type='button' className={dashboardStyles.button} disabled={busy || invitees.length === 0}
                  onClick={send}>
            {busy ? 'Working…' : 'Send invites'}
          </button>
        </div>
      </div>
    </section>
  );
}
