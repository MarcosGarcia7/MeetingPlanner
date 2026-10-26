'use client';

import Link from 'next/link';
import {useState} from 'react';
import {useSearchParams} from 'next/navigation';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import authStyles from '@/app/components/auth/auth.module.css';
import {readApiError} from '@/lib/api-error';
import {passwordSchema} from '@/lib/password-schema';
import type {ResetPasswordRequest} from '@/lib/types/auth';

const resetPasswordSchema = z.object({
  newPassword: passwordSchema,
  confirm: z.string().min(1, 'Confirm your new password'),
}).refine((data) => data.newPassword === data.confirm, {message: 'Passwords do not match', path: ['confirm']});
type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

type Status =
  | { kind: 'editing' }
  | { kind: 'success' }
  | { kind: 'invalid_token'; message: string };

export default function ResetPasswordForm() {
  const token = useSearchParams().get('token');
  const [status, setStatus] = useState<Status>({kind: 'editing'});
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: {errors, isSubmitting},
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {newPassword: '', confirm: ''},
  });

  // The token is only consumed on submit, so link scanners that prefetch the page don't burn it.
  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) return;
    setError(null);
    const body: ResetPasswordRequest = {token, newPassword: data.newPassword};

    try {
      const response = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const err = await readApiError(response);
        if (err?.error === 'invalid_token') {
          setStatus({kind: 'invalid_token', message: err.message});
        } else {
          setError(err?.message ?? 'Could not reset your password. Please try again.');
        }
        return;
      }

      setStatus({kind: 'success'});
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  if (!token || status.kind === 'invalid_token') {
    return (
      <>
        <h1 className={authStyles.title}>Reset failed</h1>
        <p className={authStyles.error} role='alert'>
          {status.kind === 'invalid_token'
            ? `${status.message}. Request a new link to try again.`
            : 'This reset link is missing its token.'}
        </p>
        <Link className={authStyles.linkButton} href='/forgot-password'>
          Request a new link
        </Link>
      </>
    );
  }

  if (status.kind === 'success') {
    return (
      <>
        <h1 className={authStyles.title}>Reset success</h1>
        <p className={authStyles.subtitle}>
          Your password has been reset and your other devices were signed out. Sign in with your new password.
        </p>
        <Link className={authStyles.linkButton} href='/login'>
          Go to login
        </Link>
      </>
    );
  }

  return (
    <>
      <h1 className={authStyles.title}>Choose a new password</h1>
      <form className={authStyles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <label className={authStyles.field}>
          <span className={authStyles.label}>New password</span>
          <input
            className={authStyles.input}
            type='password'
            autoComplete='new-password'
            {...register('newPassword')}
          />
        </label>
        {errors.newPassword && (
          <p className={authStyles.error} role='alert'>
            {errors.newPassword.message}
          </p>
        )}
        <label className={authStyles.field}>
          <span className={authStyles.label}>Confirm new password</span>
          <input
            className={authStyles.input}
            type='password'
            autoComplete='new-password'
            {...register('confirm')}
          />
        </label>
        {errors.confirm && (
          <p className={authStyles.error} role='alert'>
            {errors.confirm.message}
          </p>
        )}

        {error && (
          <p className={authStyles.error} role='alert'>
            {error}
          </p>
        )}

        <button className={authStyles.submit} type='submit' disabled={isSubmitting}>
          {isSubmitting ? 'Saving...' : 'Reset password'}
        </button>
      </form>
    </>
  );
}
