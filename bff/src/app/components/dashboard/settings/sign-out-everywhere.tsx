'use client';

import {useState} from 'react';
import type {LogoutOptions} from '@/lib/types/auth';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './settings.module.css';

export default function SignOutEverywhere() {
  const [pending, setPending] = useState(false);

  const signOut = async () => {
    setPending(true);
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({allSessions: true} satisfies LogoutOptions),
      });
    } finally {
      // Full page load, like the header's log out button, so nothing from this account lingers.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- reload is the point
      window.location.assign('/login');
    }
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='sessions-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='sessions-title' className={dashboardStyles.cardTitle}>Sessions</h2>
      </div>
      <p className={dashboardStyles.meta}>
        Sign out on every device and browser, including this one. Other devices are signed out the next time
        their session refreshes.
      </p>
      <div className={styles.formFooter}>
        <ConfirmButton label='Sign out of all devices' confirmLabel='Sign out everywhere' disabled={pending}
                       onConfirm={signOut}/>
      </div>
    </section>
  );
}
