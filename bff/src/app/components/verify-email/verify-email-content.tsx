'use client';

import {useEffect, useRef, useState} from 'react';
import Link from 'next/link';
import {useSearchParams} from 'next/navigation';
import styles from '@/app/components/auth/auth.module.css';
import {readApiError} from '@/lib/api-error';
import type {VerifyEmailRequest} from '@/lib/types/auth';

type Status =
  | { kind: 'loading' }
  | { kind: 'success' }
  | { kind: 'error'; message: string };

const FAILED_MESSAGE = 'Verification failed. The link may be invalid or expired.';

export default function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<Status>({kind: 'loading'});
  // The token is single-use, so StrictMode's second effect run must not send it again.
  const sentToken = useRef<string | null>(null);

  useEffect(() => {
    if (!token || sentToken.current === token) return;
    sentToken.current = token;

    const body: VerifyEmailRequest = {token};
    fetch('/api/auth/verify-email', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(body),
    })
      .then(async (response) => {
        if (response.ok) {
          setStatus({kind: 'success'});
          return;
        }
        const err = await readApiError(response);
        setStatus({kind: 'error', message: err?.message ?? FAILED_MESSAGE});
      })
      .catch(() => {
        setStatus({kind: 'error', message: FAILED_MESSAGE});
      });
  }, [token]);

  if (!token) {
    return (
      <>
        <p className={styles.error} role='alert'>
          This verification link is missing its token.
        </p>
        <Link className={styles.linkButton} href='/login'>
          Back to login
        </Link>
      </>
    );
  }

  if (status.kind === 'loading') {
    return <p className={styles.subtitle}>Verifying your email address...</p>;
  }

  if (status.kind === 'success') {
    return (
      <>
        <p className={styles.subtitle}>
          Your email has been verified. You can now sign in.
        </p>
        <Link className={styles.linkButton} href='/login'>
          Go to login
        </Link>
      </>
    );
  }

  return (
    <>
      <p className={styles.error} role='alert'>
        {status.message}
      </p>
      <Link className={styles.linkButton} href='/login'>
        Back to login
      </Link>
    </>
  );
}
