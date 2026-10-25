'use client';

import Image from 'next/image';
import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useRouter } from 'next/navigation';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import styles from './login-form.module.css';
import { readApiError } from '@/lib/api-error';
import type { LoginRequest, VerifyEmailResendRequest } from '@/lib/types/auth';

const loginSchema = z.object({
  identifier: z.string().trim().min(1, 'Enter your email or username'),
  password: z
    .string()
    .min(1, 'Enter your password')
    .max(128, 'Password cannot exceed 128 characters'),
});
type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginForm() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [unverified, setUnverified] = useState(false);
  const [resendSent, setResendSent] = useState(false);
  const [resendPending, setResendPending] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: '', password: '' },
  });

  const onSubmit = async (data: LoginFormData) => {
    setError(null);
    setUnverified(false);
    setResendSent(false);

    const body: LoginRequest = { identifier: data.identifier, password: data.password };

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const err = await readApiError(response);
        if (err?.error === 'email_not_verified') {
          setUnverified(true);
        } else {
          setError(err?.message ?? 'Login failed. Please try again.');
        }
        return;
      }

      router.push('/dashboard');
      router.refresh();
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  async function handleResend() {
    if (resendPending) return;

    setResendPending(true);
    setError(null);

    const body: VerifyEmailResendRequest = { email: getValues('identifier').trim() };

    try {
      const response = await fetch('/api/auth/verify-email/resend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const err = await readApiError(response);
        setError(err?.message ?? 'Failed to resend the verification email.');
        return;
      }

      setResendSent(true);
    } catch {
      setError('Something went wrong. Please try again.');
    } finally {
      setResendPending(false);
    }
  }

  const isEmail = useWatch({ control, name: 'identifier' }).includes('@');

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
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
      <label className={styles.field}>
        <span className={styles.label}>Email or username</span>
        <input
          className={styles.input}
          autoComplete='username'
          {...register('identifier')}
        />
      </label>
      {errors.identifier && (
        <p className={styles.error} role='alert'>
          {errors.identifier.message}
        </p>
      )}
      <label className={styles.field}>
        <span className={styles.label}>Password</span>
        <input
          className={styles.input}
          type='password'
          autoComplete='current-password'
          {...register('password')}
        />
      </label>
      {errors.password && (
        <p className={styles.error} role='alert'>
          {errors.password.message}
        </p>
      )}

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

      <button className={styles.submit} type='submit' disabled={isSubmitting}>
        {isSubmitting ? 'Logging in...' : 'Log in'}
      </button>
    </form>
      </div>
  );
}
