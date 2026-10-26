'use client';

import Image from 'next/image';
import Link from 'next/link';
import {useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import authStyles from '@/app/components/auth/auth.module.css';
import styles from './signup-form.module.css';
import {readApiError} from '@/lib/api-error';
import {passwordSchema} from '@/lib/password-schema';
import type {AccountCreatedResponse, CreateAccountRequest} from '@/lib/types/auth';

const credentialsSchema = z.object({
  email: z
    .email({message: 'Invalid email address'})
    .trim()
    .toLowerCase()
    .max(254, 'Email cannot exceed 254 characters'),
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
  password: passwordSchema,
  confirm: z.string().min(1, 'Confirm your password'),
});

const profileSchema = z.object({
  firstName: z.string().trim().max(50, 'First name cannot exceed 50 characters').optional(),
  lastName: z.string().trim().max(50, 'Last name cannot exceed 50 characters').optional(),
  isPrivate: z.boolean(),
  timezone: z.string().min(1, 'Select a timezone'),
});

const fullFormSchema = z.object({
  ...credentialsSchema.shape,
  ...profileSchema.shape,
}).refine((data) => data.password === data.confirm, {
  message: 'Passwords do not match',
  path: ['confirm'],
  // Run even when other fields (e.g. email) are invalid, so every step-1 error shows at once.
  when: (payload) =>
    credentialsSchema.pick({password: true, confirm: true}).safeParse(payload.value).success,
});
type CreateFormData = z.infer<typeof fullFormSchema>;

function detectTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

export default function SignupForm() {
  const router = useRouter();
  const [step, setStep] = useState<1 | 2>(1);
  const [error, setError] = useState<string | null>(null);
  const [detectedTimezone] = useState(detectTimezone);

  // Only rendered in step 2, which never server-renders, so browser and server ICU can't mismatch.
  const timezones = useMemo(() => {
    const zones = Intl.supportedValuesOf('timeZone');
    return zones.includes(detectedTimezone) ? zones : [detectedTimezone, ...zones];
  }, [detectedTimezone]);

  const {
    register,
    handleSubmit,
    formState: {errors, isSubmitting},
  } = useForm<CreateFormData>({
    resolver: zodResolver(fullFormSchema),
    defaultValues: {
      email: '',
      username: '',
      password: '',
      confirm: '',
      firstName: '',
      lastName: '',
      isPrivate: false,
      timezone: detectedTimezone,
    },
  });

  const onSubmit = async (data: CreateFormData) => {
    if (step === 1) {
      setStep(2);
      return;
    }

    setError(null);
    const body: CreateAccountRequest = {
      email: data.email,
      password: data.password,
      username: data.username || undefined,
      firstName: data.firstName || undefined,
      lastName: data.lastName || undefined,
      isPrivate: data.isPrivate,
      timezone: data.timezone,
    };

    try {
      const response = await fetch('/api/auth/create', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const err = await readApiError(response);
        setError(err?.message ?? 'Sign up failed. Please try again.');
        return;
      }

      const result = (await response.json().catch(() => null)) as AccountCreatedResponse | null;
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
        className={authStyles.logo}
        src='/logo-mp.svg'
        alt='Meeting Planner logo'
        width={64}
        height={64}
        priority
      />
      <h1 className={authStyles.title}>Create your account</h1>
      <form onSubmit={handleSubmit(onSubmit)} className={authStyles.form}>
        {step === 1 && (
          <div className={styles.step}>
            <label className={authStyles.field}>
              <span className={authStyles.label}>Email *</span>
              <input
                type='email'
                autoComplete='email'
                {...register('email')}
                className={authStyles.input}
              />
            </label>
            {errors.email && (
              <p className={authStyles.error} role='alert'>
                {errors.email.message}
              </p>
            )}

            <label className={authStyles.field}>
              <span className={authStyles.label}>Username (optional)</span>
              <input
                type='text'
                autoComplete='username'
                {...register('username')}
                className={authStyles.input}
                placeholder='Leave blank to auto-generate'
              />
            </label>
            {errors.username && (
              <p className={authStyles.error} role='alert'>
                {errors.username.message}
              </p>
            )}

            <label className={authStyles.field}>
              <span className={authStyles.label}>Password *</span>
              <input
                type='password'
                autoComplete='new-password'
                {...register('password')}
                className={authStyles.input}
              />
            </label>
            {errors.password && (
              <p className={authStyles.error} role='alert'>
                {errors.password.message}
              </p>
            )}

            <label className={authStyles.field}>
              <span className={authStyles.label}>Confirm Password *</span>
              <input
                type='password'
                autoComplete='new-password'
                {...register('confirm')}
                className={authStyles.input}
              />
            </label>
            {errors.confirm && (
              <p className={authStyles.error} role='alert'>
                {errors.confirm.message}
              </p>
            )}

            <button type='submit' className={authStyles.submit}>
              Next
            </button>
          </div>
        )}
        {step === 2 && (
          <div className={styles.step}>
            <label className={authStyles.field}>
              <span className={authStyles.label}>First Name (optional)</span>
              <input
                type='text'
                autoComplete='given-name'
                {...register('firstName')}
                className={authStyles.input}
              />
            </label>
            {errors.firstName && (
              <p className={authStyles.error} role='alert'>
                {errors.firstName.message}
              </p>
            )}

            <label className={authStyles.field}>
              <span className={authStyles.label}>Last Name (optional)</span>
              <input
                type='text'
                autoComplete='family-name'
                {...register('lastName')}
                className={authStyles.input}
              />
            </label>
            {errors.lastName && (
              <p className={authStyles.error} role='alert'>
                {errors.lastName.message}
              </p>
            )}

            <label className={authStyles.field}>
              <span className={authStyles.label}>Timezone *</span>
              <select {...register('timezone')} className={authStyles.input}>
                {timezones.map((zone) => (
                  <option key={zone} value={zone}>
                    {zone.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
            </label>
            {errors.timezone && (
              <p className={authStyles.error} role='alert'>
                {errors.timezone.message}
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
              <button type='submit' disabled={isSubmitting} className={authStyles.submit}>
                {isSubmitting ? 'Submitting...' : 'Submit'}
              </button>
            </div>
          </div>
        )}
        {error && (
          <p className={authStyles.error} role='alert'>
            {error}
          </p>
        )}
      </form>
      <p className={authStyles.altLink}>
        Already have an account? <Link href='/login'>Log in</Link>
      </p>
    </div>
  );
}
