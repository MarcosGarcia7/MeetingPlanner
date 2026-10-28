'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {readApiError} from '@/lib/api-error';
import type {DeleteAccountRequest} from '@/lib/types/user';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './settings.module.css';

const deleteAccountSchema = z.object({
  currentPassword: z.string().min(1, 'Enter your password.'),
});

type DeleteAccountFormData = z.infer<typeof deleteAccountSchema>;

export default function DeleteAccount({username}: { username: string }) {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError: setFieldError,
    formState: {errors, isSubmitting},
  } = useForm<DeleteAccountFormData>({
    resolver: zodResolver(deleteAccountSchema),
    defaultValues: {currentPassword: ''},
  });

  const cancel = () => {
    reset();
    setError(null);
    setOpen(false);
  };

  const onSubmit = async (data: DeleteAccountFormData) => {
    setError(null);
    const body: DeleteAccountRequest = {currentPassword: data.currentPassword};

    try {
      const response = await fetch('/api/users/me', {
        method: 'DELETE',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        if (err?.error === 'incorrect_password') {
          setFieldError('currentPassword', {message: 'Password is incorrect.'});
        } else {
          setError(err?.message ?? 'Could not delete your account. Please try again.');
        }
        return;
      }
      // Full page load: the session is gone, so nothing from this account should stay in memory.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- reload is the point
      window.location.assign('/');
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  return (
    <section className={styles.dangerCard} aria-labelledby='delete-account-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='delete-account-title' className={dashboardStyles.cardTitle}>Delete account</h2>
      </div>
      <p className={dashboardStyles.meta}>
        Permanently deletes your account, along with the groups you own, the meetings you created, and your
        posts. This can’t be undone.
      </p>

      {open ? (
        <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
          {/* Lets password managers offer the saved password for this account. */}
          <input type='text' autoComplete='username' value={username} readOnly hidden/>

          <label className={authStyles.field}>
            <span className={authStyles.label}>Enter your password to confirm</span>
            <input type='password' autoComplete='current-password' {...register('currentPassword')}
                   className={authStyles.input}/>
            {errors.currentPassword && (
              <span className={authStyles.error} role='alert'>{errors.currentPassword.message}</span>
            )}
          </label>

          {error && <p className={authStyles.error} role='alert'>{error}</p>}
          <div className={styles.formFooter}>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={isSubmitting} onClick={cancel}>
              Cancel
            </button>
            <button type='submit' className={dashboardStyles.buttonDanger} disabled={isSubmitting}>
              {isSubmitting ? 'Deleting…' : 'Delete my account'}
            </button>
          </div>
        </form>
      ) : (
        <div className={styles.formFooter}>
          <button type='button' className={dashboardStyles.buttonDanger} onClick={() => setOpen(true)}>
            Delete account…
          </button>
        </div>
      )}
    </section>
  );
}
