'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useRouter } from 'next/navigation';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import styles from './signup-form.module.css';
import Image from "next/image";

const credentialsSchema = z.object({
  email: z.email({ message: 'Invalid email address' }).trim().toLowerCase(),
  username: z
    .union([
      z.literal(''),
      z
        .string()
        .trim()
        .min(1)
        .max(20, 'Username cannot exceed 20 characters')
        .regex(
          /^[A-Za-z0-9_-]+$/,
          'Username may only contain letters, digits, hyphens, and underscores'
        ),
    ]),
  password: z
    .string()
    .min(12, 'Password must be at least 12 characters')
    .max(72, 'Password cannot exceed 72 characters'),
});

const profileSchema = z.object({
  firstName: z.string().trim().max(50, 'First name cannot exceed 50 characters').optional(),
  lastName: z.string().trim().max(50, 'Last name cannot exceed 50 characters').optional(),
  isPrivate: z.boolean(),
});

const fullFormSchema = z.object({
  ...credentialsSchema.shape,
  ...profileSchema.shape,
});
type CreateFormData = z.infer<typeof fullFormSchema>;

export default function SignupForm() {
  const router = useRouter();
  const [step, setStep] = useState<1 | 2>(1);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateFormData>({
    resolver: zodResolver(fullFormSchema),
    defaultValues: {
      email: '',
      username: '',
      password: '',
      firstName: '',
      lastName: '',
      isPrivate: false,
    },
  });

  const onSubmit = async (data: CreateFormData) => {
    if (step === 1) {
      setStep(2);
      return;
    }

    try {
      const body = new FormData();

      Object.entries(data).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          body.append(key, String(value));
        }
      });

      const response = await fetch('/api/auth/create', {
        method: 'POST',
        body,
      });

      if (!response.ok) {
        const data = (await response.json().catch(() => null)) as
          | { message?: string }
          | null;
        setError(data?.message ?? 'Sign up failed. Please try again.');
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | { email?: string }
        | null;
      router.push(
        `/verify-email/sent?email=${encodeURIComponent(result?.email ?? data.email)}`
      );
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

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
      <h1 className={styles.title}>Create your account</h1>
      <form onSubmit={handleSubmit(onSubmit)} className={styles.form}>
        {step === 1 && (
          <div>
            <label className={styles.field}>
              <span className={styles.label}>Email *</span>
              <input
                type='email'
                autoComplete='email'
                {...register('email')}
                className={styles.input}
              />
            </label>
            {errors.email && (
              <p className={styles.error} role='alert'>
                {errors.email.message}
              </p>
            )}

            <label className={styles.field}>
              <span className={styles.label}>Username (optional)</span>
              <input
                type='text'
                autoComplete='username'
                {...register('username')}
                className={styles.input}
                placeholder='Leave blank to auto-generate'
              />
            </label>
            {errors.username && (
              <p className={styles.error} role='alert'>
                {errors.username.message}
              </p>
            )}

            <label className={styles.field}>
              <span className={styles.label}>Password *</span>
              <input
                type='password'
                autoComplete='new-password'
                {...register('password')}
                className={styles.input}
              />
            </label>
            {errors.password && (
              <p className={styles.error} role='alert'>
                {errors.password.message}
              </p>
            )}

            <button type='submit' className={styles.submit}>
              Next
            </button>
          </div>
        )}
        {step === 2 && (
          <div>
            <label className={styles.field}>
              <span className={styles.label}>First Name (optional)</span>
              <input
                type='text'
                autoComplete='given-name'
                {...register('firstName')}
                className={styles.input}
              />
            </label>
            {errors.firstName && (
              <p className={styles.error} role='alert'>
                {errors.firstName.message}
              </p>
            )}

            <label className={styles.field}>
              <span className={styles.label}>Last Name (optional)</span>
              <input
                type='text'
                autoComplete='family-name'
                {...register('lastName')}
                className={styles.input}
              />
            </label>
            {errors.lastName && (
              <p className={styles.error} role='alert'>
                {errors.lastName.message}
              </p>
            )}

            <label className={styles.checkRow}>
              <input type='checkbox' {...register('isPrivate')} />
              <span>Make my account private</span>
            </label>

            <div className={styles.actions}>
              <button
                type='button'
                onClick={() => setStep(1)}
                disabled={isSubmitting}
                className={styles.back}
              >
                Back
              </button>
              <button type='submit' disabled={isSubmitting} className={styles.submit}>
                {isSubmitting ? 'Submitting...' : 'Submit'}
              </button>
            </div>
          </div>
        )}
        {error && (
          <p className={styles.error} role='alert'>
            {error}
          </p>
        )}
      </form>
    </div>
  );
}