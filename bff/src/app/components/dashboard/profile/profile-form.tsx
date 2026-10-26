'use client';

import Image from 'next/image';
import {useState, useSyncExternalStore} from 'react';
import {useForm, useWatch} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {avatarDataUri} from '@/lib/avatar';
import {readApiError} from '@/lib/api-error';
import type {UserProfileResponse, UserProfileUpdateRequest} from '@/lib/types/user';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import {AVATAR_COLORS} from './avatar-colors';
import styles from './profile.module.css';

const profileSchema = z.object({
  firstName: z.string().trim().max(50, 'First name cannot exceed 50 characters'),
  lastName: z.string().trim().max(50, 'Last name cannot exceed 50 characters'),
  avatarColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Pick a color'),
  timezone: z.string(),
});
type ProfileFormData = z.infer<typeof profileSchema>;

function toFormData(profile: UserProfileResponse): ProfileFormData {
  return {
    firstName: profile.firstName ?? '',
    lastName: profile.lastName ?? '',
    avatarColor: profile.avatarColor.toUpperCase(),
    timezone: profile.timezone ?? '',
  };
}

// The zone list differs between Node's ICU and the browser's, so it is only read on the client; the
// server render (and hydration) lists just the saved zone.
let timezoneCache: string[] | null = null;
const subscribeNever = () => () => {};
const clientTimezones = () => (timezoneCache ??= Intl.supportedValuesOf('timeZone'));
const serverTimezones = () => null;

export default function ProfileForm({profile}: { profile: UserProfileResponse }) {
  const router = useRouter();
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const allTimezones = useSyncExternalStore(subscribeNever, clientTimezones, serverTimezones);

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: {errors, isSubmitting, isDirty, dirtyFields},
  } = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: toFormData(profile),
  });

  const [firstName, lastName, avatarColor, timezone] = useWatch({control, name: ['firstName', 'lastName', 'avatarColor', 'timezone']});
  const savedTimezone = profile.timezone ?? '';
  const timezones = [...new Set([savedTimezone, timezone, ...(allTimezones ?? [])])].filter(Boolean);
  const isPreset = (AVATAR_COLORS as readonly string[]).includes(avatarColor);
  const preview = avatarDataUri({
    username: profile.username,
    firstName: firstName.trim() || null,
    lastName: lastName.trim() || null,
    avatarColor,
  });

  const onSubmit = async (data: ProfileFormData) => {
    setStatus(null);
    setError(null);
    // PATCH semantics: omitted fields stay as they are, so send only what changed ('' clears a field).
    const body: UserProfileUpdateRequest = {
      firstName: dirtyFields.firstName ? data.firstName : undefined,
      lastName: dirtyFields.lastName ? data.lastName : undefined,
      avatarColor: dirtyFields.avatarColor ? data.avatarColor : undefined,
      timezone: dirtyFields.timezone ? data.timezone : undefined,
    };

    try {
      const response = await fetch('/api/users/me', {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        setError(err?.message ?? 'Could not save your profile. Please try again.');
        return;
      }

      reset(toFormData((await response.json()) as UserProfileResponse));
      setStatus('Profile saved.');
      router.refresh();
    } catch {
      setError('Something went wrong. Please try again.');
    }
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='profile-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='profile-title' className={dashboardStyles.cardTitle}>Profile</h2>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
        <div className={styles.row}>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Username</span>
            <input type='text' value={`@${profile.username}`} disabled className={styles.readOnlyInput}/>
            <span className={styles.hint}>Usernames can’t be changed.</span>
          </label>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Timezone</span>
            <select {...register('timezone')} className={authStyles.input}>
              <option value=''>Not set (UTC)</option>
              {timezones.map((zone) => <option key={zone} value={zone}>{zone.replaceAll('_', ' ')}</option>)}
            </select>
          </label>
        </div>

        <div className={authStyles.field}>
          <span className={authStyles.label} id='avatar-color-label'>Avatar color</span>
          <div className={styles.colorPicker}>
            <Image src={preview} alt='Avatar preview' width={56} height={56} className={styles.avatar} unoptimized/>
            <fieldset className={styles.swatches} aria-labelledby='avatar-color-label'>
              {AVATAR_COLORS.map((color) => (
                <label key={color} className={styles.swatch}>
                  <input type='radio' value={color} {...register('avatarColor')} className={styles.visuallyHidden}/>
                  <Image
                    src={avatarDataUri({username: profile.username, firstName: profile.firstName, lastName: profile.lastName, avatarColor: color})}
                    alt={color}
                    width={32}
                    height={32}
                    className={styles.avatar}
                    unoptimized
                  />
                </label>
              ))}
            </fieldset>
            <label className={styles.customColor}>
              <input
                type='color'
                value={avatarColor.toLowerCase()}
                onChange={(event) =>
                  setValue('avatarColor', event.target.value.toUpperCase(), {shouldDirty: true, shouldValidate: true})}
                className={styles.colorInput}
              />
              {isPreset ? 'Custom' : avatarColor}
            </label>
          </div>
          {errors.avatarColor && <span className={authStyles.error} role='alert'>{errors.avatarColor.message}</span>}
        </div>

        <div className={styles.row}>
          <label className={authStyles.field}>
            <span className={authStyles.label}>First name</span>
            <input type='text' autoComplete='given-name' {...register('firstName')} className={authStyles.input}/>
            {errors.firstName && <span className={authStyles.error} role='alert'>{errors.firstName.message}</span>}
          </label>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Last name</span>
            <input type='text' autoComplete='family-name' {...register('lastName')} className={authStyles.input}/>
            {errors.lastName && <span className={authStyles.error} role='alert'>{errors.lastName.message}</span>}
          </label>
        </div>

        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          {status && !isDirty && <span className={styles.status} role='status'>{status}</span>}
          <button
            type='button'
            className={dashboardStyles.buttonSecondary}
            disabled={!isDirty || isSubmitting}
            onClick={() => {
              reset();
              setError(null);
            }}
          >
            Cancel
          </button>
          <button type='submit' className={dashboardStyles.button} disabled={!isDirty || isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </section>
  );
}
