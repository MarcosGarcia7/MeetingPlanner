'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {GroupInviteBatchRequest} from '@/lib/types/group';
import type {InviteBatchResponse} from '@/lib/types/invite';
import {sendJson} from '@/lib/send-json';
import InviteeInput from './invitee-input';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

// Invites people to the group by username. Reports who was invited and why anyone wasn't.
export default function GroupInviteForm({groupId}: { groupId: number }) {
  const router = useRouter();
  const [usernames, setUsernames] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState<string | null>(null);
  const [failures, setFailures] = useState<string[]>([]);

  async function send() {
    setBusy(true);
    setSent(null);
    setFailures([]);
    const body: GroupInviteBatchRequest = {recipients: usernames.map((username) => ({username}))};
    const result = await sendJson<InviteBatchResponse>(`/api/groups/${groupId}/invites`, 'POST', body);
    setBusy(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      // Every recipient failed (400 whose message lists each reason): keep the list so it can be fixed.
      setFailures([result.message]);
      return;
    }
    const {invited, failed} = result.data;
    setSent(`Invited ${invited.length} ${invited.length === 1 ? 'person' : 'people'}.`);
    setFailures(failed.map((f) => `@${f.username}: ${f.reason ?? 'not invited'}`));
    // Keep only the ones that failed, so they can be corrected and resent.
    setUsernames(failed.flatMap((f) => (f.username ? [f.username] : [])));
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='group-invite-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='group-invite-title' className={dashboardStyles.cardTitle}>Invite people</h2>
      </div>
      <div className={styles.manageBody}>
        <InviteeInput value={usernames} onChange={setUsernames} suggestions={[]} disabled={busy}
                      allowEmail={false} label='Usernames'/>
        {sent && <p className={dashboardStyles.meta} role='status'>{sent}</p>}
        {failures.map((failure) => <p key={failure} className={dashboardStyles.error}>{failure}</p>)}
        <div className={styles.formFooter}>
          <button type='button' className={dashboardStyles.button} disabled={busy || usernames.length === 0}
                  onClick={send}>
            {busy ? 'Sending…' : 'Send invites'}
          </button>
        </div>
      </div>
    </section>
  );
}
