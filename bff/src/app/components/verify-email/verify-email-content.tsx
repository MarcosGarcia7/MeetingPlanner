'use client';

import {useEffect, useState} from 'react';
import Link from 'next/link';
import {useSearchParams} from 'next/navigation';
import styles from '@/app/components/auth/login/login-form.module.css';
import {readApiError} from '@/lib/api-error';
import type {VerifyEmailRequest} from '@/lib/types/auth';

type Status =
  | { kind: 'loading' }
  | { kind: 'success' }
  | { kind: 'error'; message: string };

export default function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<Status>({kind: 'loading'});

  useEffect(() => {
    if (!token) return;

    let cancelled = false;
    const body: VerifyEmailRequest = {token};
    fetch('/api/auth/verify-email', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(body),
    })
      .then(async (response) => {
        if (cancelled) return;
        if (response.ok) {
          setStatus({kind: 'success'});
          return;
        }
        const err = await readApiError(response);
        setStatus({
          kind: 'error',
          message: err?.message ?? 'Verification failed. The link may be invalid or expired.',
        });
      })
      .catch(() => {
        if (!cancelled) {
          setStatus({
            kind: 'error',
            message: 'Verification failed. The link may be invalid or expired.',
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  if (!token) {
    return (
      <>
        <p className={styles.error} role='alert'>
          This verification link is missing its token.
        </p>
        <Link
          className={styles.submit}
          href='/login'
        >
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
        <Link
          className={styles.submit}
          href='/login'
        >
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
      <Link
        className={styles.submit}
        href='/login'
      >
        Back to login
      </Link>
    </>
  );
}
