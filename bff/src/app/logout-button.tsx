'use client';

import {useRouter} from 'next/navigation';
import {useState} from 'react';
import styles from './header.module.css';

export default function LogoutButton() {
  const router = useRouter();
  const [pending, setPending] = useState(false);

  async function handleLogout() {
    if (pending) return;
    setPending(true);
    try {
      await fetch('/api/auth/logout', {method: 'POST'});
    } finally {
      setPending(false);
      router.push('/');
      router.refresh();
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
