'use client';

import Image from 'next/image';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import styles from './login-form.module.css';

export default function LoginForm() {
  const router = useRouter();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [unverified, setUnverified] = useState(false);
  const [resendSent, setResendSent] = useState(false);
  const [resendPending, setResendPending] = useState(false);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: React.SubmitEvent) {
    e.preventDefault();
    if (pending) return;

    setPending(true);
    setError(null);
    setUnverified(false);
    setResendSent(false);

    try {
      const body = new FormData();
      body.set('identifier', identifier);
      body.set('password', password);

      const response = await fetch('/api/auth/login', {
        method: 'POST',
        body,
      });

      if (!response.ok) {
        const data = (await response.json().catch(() => null)) as
          | { error?: string; message?: string }
          | null;

        if (data?.error === 'email_not_verified') {
          setUnverified(true);
        } else {
          setError(data?.message ?? 'Login failed. Please try again.');
        }
        return;
      }

      router.push('/dashboard');
      router.refresh();
    } finally {
      setPending(false);
    }
  }

  async function handleResend() {
    if (resendPending) return;

    setResendPending(true);
    setError(null);

    try {
      const response = await fetch('/api/auth/verify-email/resend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: identifier }),
      });

      if (!response.ok) {
        const data = (await response.json().catch(() => null)) as
          | { message?: string }
          | null;
        setError(data?.message ?? 'Failed to resend the verification email.');
        return;
      }

      setResendSent(true);
    } finally {
      setResendPending(false);
    }
  }

  const isEmail = identifier.includes('@');

  return (
      <div>
        <Image
            className={styles.logo}
            src='/logo-mp.svg'
            alt='Meeting Planner logo'
            width={64}
            height={64}
            priority
        />
        <h1 className={styles.title}>Login</h1>
    <form className={styles.form} onSubmit={handleSubmit}>
      <label className={styles.field}>
        <span className={styles.label}>Email or username</span>
        <input
          className={styles.input}
          autoComplete='username'
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          required
        />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>Password</span>
        <input
          className={styles.input}
          type='password'
          autoComplete='current-password'
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </label>

      {error && (
        <p className={styles.error} role='alert'>
          {error}
        </p>
      )}

      {unverified && (
        <div className={styles.error} role='alert'>
          {resendSent ? (
            <span>Verification email resent. Check your inbox before signing in.</span>
          ) : isEmail ? (
            <>
              <span>Your email has not been verified yet. Check your inbox or resend the link.</span>
              <button
                className={styles.resendLink}
                type='button'
                disabled={resendPending}
                onClick={handleResend}
              >
                {resendPending ? 'Sending...' : 'Resend verification link'}
              </button>
            </>
          ) : (
            <span>Your email has not been verified yet. Sign in with your email address to resend the link.</span>
          )}
        </div>
      )}

      <button className={styles.submit} type='submit' disabled={pending}>
        {pending ? 'Logging in...' : 'log in'}
      </button>
    </form>
      </div>
  );
}