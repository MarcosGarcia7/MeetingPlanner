'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {sendJson} from '@/lib/send-json';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

// Deletes the meeting for everyone; its participants are notified.
export default function DeleteMeeting({meetingId}: { meetingId: number }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function remove() {
    setBusy(true);
    setError(null);
    const result = await sendJson(`/api/meetings/${meetingId}`, 'DELETE');
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
    <section className={styles.dangerZone} aria-labelledby='delete-meeting-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='delete-meeting-title' className={dashboardStyles.cardTitle}>Delete meeting</h2>
      </div>
      <div className={styles.stack}>
        <p className={dashboardStyles.meta}>This cancels the meeting for everyone. It can’t be undone.</p>
        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          <ConfirmButton label='Delete meeting' confirmLabel='Delete for everyone' disabled={busy} onConfirm={remove}/>
        </div>
      </div>
    </section>
  );
}
