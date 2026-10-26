'use client';

import Image from 'next/image';
import Link from 'next/link';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import authStyles from '@/app/components/auth/auth.module.css';
import {readApiError} from '@/lib/api-error';
import type {ForgotPasswordRequest} from '@/lib/types/auth';

const forgotPasswordSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email').email('Enter a valid email address'),
});
type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordForm() {
  const [error, setError] = useState<string | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: {errors, isSubmitting},
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {email: ''},
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setError(null);
    const body: ForgotPasswordRequest = {email: data.email};

    try {
      const response = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const err = await readApiError(response);
        setError(err?.message ?? 'Could not send the reset link. Please try again.');
        return;
      }

      setSentTo(data.email);
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  return (
    <div>
      <Image
        className={authStyles.logo}
        src='/logo-mp.svg'
        alt='Meeting Planner logo'
        width={64}
        height={64}
        priority
      />
      <h1 className={authStyles.title}>Reset your password</h1>
      {sentTo ? (
        <>
          {/* Same message whether or not the account exists, so emails can't be probed. */}
          <p className={authStyles.subtitle}>
            If an account exists for <strong>{sentTo}</strong>, we sent it a link to reset your password.
            The link expires in 1 hour.
          </p>
          <Link className={authStyles.linkButton} href='/login'>
            Back to login
          </Link>
        </>
      ) : (
        <>
          <p className={authStyles.subtitle}>
            Enter the email for your account and we&apos;ll send you a reset link.
          </p>
          <form className={authStyles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
            <label className={authStyles.field}>
              <span className={authStyles.label}>Email</span>
              <input
                className={authStyles.input}
                type='email'
                autoComplete='email'
                {...register('email')}
              />
            </label>
            {errors.email && (
              <p className={authStyles.error} role='alert'>
                {errors.email.message}
              </p>
            )}

            {error && (
              <p className={authStyles.error} role='alert'>
                {error}
              </p>
            )}

            <button className={authStyles.submit} type='submit' disabled={isSubmitting}>
              {isSubmitting ? 'Sending...' : 'Send reset link'}
            </button>
          </form>
          <p className={authStyles.altLink}>
            Remembered it? <Link href='/login'>Back to login</Link>
          </p>
        </>
      )}
    </div>
  );
}
