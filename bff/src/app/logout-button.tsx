'use client';

import {useState} from 'react';
import styles from './header.module.css';

export default function LogoutButton() {
  const [pending, setPending] = useState(false);

  async function handleLogout() {
    if (pending) return;
    setPending(true);
    try {
      await fetch('/api/auth/logout', {method: 'POST'});
    } finally {
      // Full page load, not a client-side navigation: drops the router cache, loaded images, and
      // React state from this account so none of it can show up for the next one.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- reload is the point
      window.location.assign('/');
    }
  }

  return (
    <button
      className={styles.buttonPrimary}
      type='button'
      onClick={handleLogout}
      disabled={pending}
    >
      {pending ? 'logging out...' : 'log out'}
    </button>
  );
}
