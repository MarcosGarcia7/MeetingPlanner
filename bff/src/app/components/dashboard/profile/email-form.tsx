'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {readApiError} from '@/lib/api-error';
import type {UserProfileResponse, UserProfileUpdateRequest} from '@/lib/types/user';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './profile.module.css';

const emailSchema = z.object({
  newEmail: z
    .email({message: 'Invalid email address'})
    .trim()
    .toLowerCase()
    .max(254, 'Email cannot exceed 254 characters'),
});
type EmailFormData = z.infer<typeof emailSchema>;

export default function EmailForm({profile}: { profile: UserProfileResponse }) {
  const router = useRouter();
  const currentEmail = profile.email ?? '';
  const [pendingEmail, setPendingEmail] = useState(profile.pendingEmail);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError: setFieldError,
    formState: {errors, isSubmitting},
  } = useForm<EmailFormData>({
    resolver: zodResolver(emailSchema),
    defaultValues: {newEmail: ''},
  });

  // Setting pendingEmail sends a confirmation link; setting it back to the current email cancels.
  const requestChange = async (address: string): Promise<UserProfileResponse | null> => {
    setStatus(null);
    setError(null);
    const body: UserProfileUpdateRequest = {pendingEmail: address};

    try {
      const response = await fetch('/api/users/me', {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        if (err?.error === 'email_taken') {
          setFieldError('newEmail', {message: 'That email is already in use.'});
        } else {
          setError(err?.message ?? 'Could not update your email. Please try again.');
        }
        return null;
      }

      const updated = (await response.json()) as UserProfileResponse;
      setPendingEmail(updated.pendingEmail);
      router.refresh();
      return updated;
    } catch {
      setError('Something went wrong. Please try again.');
      return null;
    }
  };

  const onSubmit = async ({newEmail}: EmailFormData) => {
    if (newEmail === currentEmail.toLowerCase()) {
      setFieldError('newEmail', {message: 'That’s already your email.'});
      return;
    }
    if (await requestChange(newEmail)) {
      reset();
      setStatus(`Confirmation link sent to ${newEmail}.`);
    }
  };

  const runPendingAction = async (address: string, message: string) => {
    setBusy(true);
    if (await requestChange(address)) {
      setStatus(message);
    }
    setBusy(false);
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='email-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='email-title' className={dashboardStyles.cardTitle}>Email</h2>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
        <div className={authStyles.field}>
          <span className={authStyles.label}>Current email</span>
          <span className={styles.emailLine}>
            {currentEmail}
            {profile.emailVerified && <span className={styles.pill}>Verified</span>}
          </span>
        </div>

        {pendingEmail && (
          <div className={styles.notice} role='status'>
            <span className={styles.noticeText}>
              Waiting for confirmation at <strong>{pendingEmail}</strong>. Your current email stays active
              until you click the link we sent.
            </span>
            <button
              type='button'
              className={dashboardStyles.buttonSecondary}
              disabled={busy || isSubmitting}
              onClick={() => runPendingAction(pendingEmail, `Confirmation link re-sent to ${pendingEmail}.`)}
            >
              Resend
            </button>
            <button
              type='button'
              className={dashboardStyles.buttonSecondary}
              disabled={busy || isSubmitting}
              onClick={() => runPendingAction(currentEmail, 'Email change cancelled.')}
            >
              Cancel change
            </button>
          </div>
        )}

        <label className={authStyles.field}>
          <span className={authStyles.label}>New email</span>
          <input type='email' autoComplete='email' {...register('newEmail')} className={authStyles.input}/>
          {errors.newEmail && <span className={authStyles.error} role='alert'>{errors.newEmail.message}</span>}
        </label>

        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          {status && <span className={styles.status} role='status'>{status}</span>}
          <button type='submit' className={dashboardStyles.button} disabled={busy || isSubmitting}>
            {isSubmitting ? 'Sending…' : 'Send confirmation link'}
          </button>
        </div>
      </form>
    </section>
  );
}
