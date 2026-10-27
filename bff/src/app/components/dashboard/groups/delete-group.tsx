'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {sendJson} from '@/lib/send-json';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

// Owner-only. Deleting can't be undone, so the exact group name has to be typed first.
export default function DeleteGroup({groupId, groupName}: { groupId: number; groupName: string }) {
  const router = useRouter();
  const [typed, setTyped] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function remove() {
    setBusy(true);
    setError(null);
    const result = await sendJson(`/api/groups/${groupId}`, 'DELETE');
    if (!result.ok) {
      setBusy(false);
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    router.push('/dashboard/groups/mine');
    router.refresh();
  }

  return (
    <section className={styles.dangerZone} aria-labelledby='delete-group-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='delete-group-title' className={dashboardStyles.cardTitle}>Delete group</h2>
      </div>
      <div className={styles.manageBody}>
        <p className={dashboardStyles.meta}>
          This deletes the group for everyone, along with its posts, comments, and meetings. It can’t be
          undone. To hand the group to someone else instead, use Make owner on the Members tab.
        </p>
        <label className={authStyles.field}>
          <span className={authStyles.label}>Type <strong>{groupName}</strong> to confirm</span>
          <input type='text' value={typed} autoComplete='off' disabled={busy} className={authStyles.input}
                 onChange={(event) => setTyped(event.target.value)}/>
        </label>
        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          <button type='button' className={dashboardStyles.buttonDanger} disabled={busy || typed !== groupName}
                  onClick={remove}>
            {busy ? 'Deleting…' : 'Delete this group'}
          </button>
        </div>
      </div>
    </section>
  );
}
