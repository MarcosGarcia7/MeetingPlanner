'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {readApiError} from '@/lib/api-error';
import type {UserProfileResponse, UserProfileUpdateRequest} from '@/lib/types/user';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

type PrivacyField = 'isPrivate' | 'friendsCanViewSchedule';

export default function PrivacyToggles({profile}: { profile: UserProfileResponse }) {
  const router = useRouter();
  const [values, setValues] = useState<Record<PrivacyField, boolean>>({
    isPrivate: profile.isPrivate,
    friendsCanViewSchedule: profile.friendsCanViewSchedule,
  });
  const [saving, setSaving] = useState<PrivacyField | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Each switch saves on its own; the change shows immediately and rolls back if the save fails.
  const toggle = async (field: PrivacyField, value: boolean) => {
    setError(null);
    setSaving(field);
    setValues((current) => ({...current, [field]: value}));
    const body: UserProfileUpdateRequest = {[field]: value};

    try {
      const response = await fetch('/api/users/me', {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        setValues((current) => ({...current, [field]: !value}));
        setError(err?.message ?? 'Could not save that setting. Please try again.');
        return;
      }
      router.refresh();
    } catch {
      setValues((current) => ({...current, [field]: !value}));
      setError('Something went wrong. Please try again.');
    } finally {
      setSaving(null);
    }
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='privacy-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='privacy-title' className={dashboardStyles.cardTitle}>Privacy</h2>
      </div>

      <label className={dashboardStyles.setting}>
        <span className={dashboardStyles.settingText}>
          <span className={dashboardStyles.settingLabel}>Private account</span>
          <span className={dashboardStyles.meta}>
            {values.isPrivate
              ? 'Only people in groups where you share your schedule (and friends, if allowed below) can see it.'
              : 'Anyone can see your schedule.'}
          </span>
        </span>
        <input
          type='checkbox'
          role='switch'
          className={dashboardStyles.switch}
          checked={values.isPrivate}
          disabled={saving !== null}
          onChange={(event) => toggle('isPrivate', event.target.checked)}
        />
      </label>

      <label className={dashboardStyles.setting}>
        <span className={dashboardStyles.settingText}>
          <span className={dashboardStyles.settingLabel}>Friends can view my schedule</span>
          <span className={dashboardStyles.meta}>
            {values.isPrivate
              ? 'Let friends see your schedule even though your account is private.'
              : 'Only applies to private accounts.'}
          </span>
        </span>
        <input
          type='checkbox'
          role='switch'
          className={dashboardStyles.switch}
          checked={values.friendsCanViewSchedule}
          disabled={saving !== null || !values.isPrivate}
          onChange={(event) => toggle('friendsCanViewSchedule', event.target.checked)}
        />
      </label>

      {error && <p className={authStyles.error} role='alert'>{error}</p>}
    </section>
  );
}
