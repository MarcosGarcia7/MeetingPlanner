'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {readApiError} from '@/lib/api-error';
import {passwordSchema} from '@/lib/password-schema';
import type {ChangePasswordRequest} from '@/lib/types/user';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './profile.module.css';

const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Enter your current password'),
  newPassword: passwordSchema,
  confirm: z.string().min(1, 'Confirm your new password'),
})
  .refine((data) => data.newPassword === data.confirm, {message: 'Passwords do not match', path: ['confirm']})
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: 'New password must be different from your current one',
    path: ['newPassword'],
  });
type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

export default function PasswordForm({username}: { username: string }) {
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError: setFieldError,
    formState: {errors, isSubmitting},
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {currentPassword: '', newPassword: '', confirm: ''},
  });

  const onSubmit = async (data: ChangePasswordFormData) => {
    setStatus(null);
    setError(null);
    const body: ChangePasswordRequest = {currentPassword: data.currentPassword, newPassword: data.newPassword};

    try {
      const response = await fetch('/api/users/me/password', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        if (err?.error === 'incorrect_password') {
          setFieldError('currentPassword', {message: 'Current password is incorrect.'});
        } else {
          setError(err?.message ?? 'Could not change your password. Please try again.');
        }
        return;
      }

      reset();
      setStatus('Password updated. Your other devices were signed out.');
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='password-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='password-title' className={dashboardStyles.cardTitle}>Password</h2>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
        {/* Lets password managers file the new password under the right account. */}
        <input type='text' autoComplete='username' value={username} readOnly hidden/>

        <label className={authStyles.field}>
          <span className={authStyles.label}>Current password</span>
          <input type='password' autoComplete='current-password' {...register('currentPassword')}
                 className={authStyles.input}/>
          {errors.currentPassword && (
            <span className={authStyles.error} role='alert'>{errors.currentPassword.message}</span>
          )}
        </label>
        <div className={styles.row}>
          <label className={authStyles.field}>
            <span className={authStyles.label}>New password</span>
            <input type='password' autoComplete='new-password' {...register('newPassword')}
                   className={authStyles.input}/>
            {errors.newPassword && <span className={authStyles.error} role='alert'>{errors.newPassword.message}</span>}
          </label>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Confirm new password</span>
            <input type='password' autoComplete='new-password' {...register('confirm')} className={authStyles.input}/>
            {errors.confirm && <span className={authStyles.error} role='alert'>{errors.confirm.message}</span>}
          </label>
        </div>
        <span className={styles.hint}>At least 12 characters. Changing it signs out your other devices.</span>

        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          {status && <span className={styles.status} role='status'>{status}</span>}
          <button type='submit' className={dashboardStyles.button} disabled={isSubmitting}>
            {isSubmitting ? 'Updating…' : 'Update password'}
          </button>
        </div>
      </form>
    </section>
  );
}
