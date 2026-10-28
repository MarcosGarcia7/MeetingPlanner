'use client';

import {useState} from 'react';
import {readApiError} from '@/lib/api-error';
import type {NotificationPreferenceUpdateRequest, NotificationPreferencesResponse} from '@/lib/types/notification';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './settings.module.css';

type CategoryField = Exclude<keyof NotificationPreferencesResponse, 'emailEnabled' | 'reminderLeadMinutes'>;

const CATEGORIES: { field: CategoryField; label: string; description: string }[] = [
  {field: 'groupInvites', label: 'Group invites', description: 'Someone invites you to a group.'},
  {field: 'meetingInvites', label: 'Meeting invites', description: 'Someone invites you to a meeting.'},
  {field: 'meetingUpdates', label: 'Meeting updates', description: 'Time changes, votes, and cancellations.'},
  {field: 'meetingReminders', label: 'Meeting reminders', description: 'A heads-up before a meeting starts.'},
  {field: 'friendRequests', label: 'Friend requests', description: 'Someone sends or accepts a friend request.'},
  {field: 'groupPosts', label: 'Group posts', description: 'New posts in your groups.'},
  {field: 'postComments', label: 'Post comments', description: 'Comments on your posts.'},
];

const LEAD_TIMES: { minutes: number; label: string }[] = [
  {minutes: 5, label: '5 minutes'},
  {minutes: 10, label: '10 minutes'},
  {minutes: 15, label: '15 minutes'},
  {minutes: 30, label: '30 minutes'},
  {minutes: 60, label: '1 hour'},
  {minutes: 120, label: '2 hours'},
  {minutes: 1440, label: '1 day'},
];

export default function NotificationPreferences({preferences}: { preferences: NotificationPreferencesResponse }) {
  const [values, setValues] = useState(preferences);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Each control saves on its own; the change shows immediately and rolls back if the save fails.
  const save = async (change: Partial<NotificationPreferencesResponse>) => {
    const previous = values;
    setError(null);
    setSaving(true);
    setValues({...values, ...change});
    const body: NotificationPreferenceUpdateRequest = change;

    try {
      const response = await fetch('/api/users/me/notification-preferences', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        setValues(previous);
        setError(err?.message ?? 'Could not save that setting. Please try again.');
        return;
      }
      setValues(await response.json() as NotificationPreferencesResponse);
    } catch {
      setValues(previous);
      setError('Something went wrong. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // A saved lead time outside the presets still shows as selected.
  const leadTimes = LEAD_TIMES.some((option) => option.minutes === values.reminderLeadMinutes)
    ? LEAD_TIMES
    : [...LEAD_TIMES, {minutes: values.reminderLeadMinutes, label: `${values.reminderLeadMinutes} minutes`}]
      .sort((a, b) => a.minutes - b.minutes);

  return (
    <>
      <section className={dashboardStyles.card} aria-labelledby='email-notifications-title'>
        <div className={dashboardStyles.cardHeader}>
          <h2 id='email-notifications-title' className={dashboardStyles.cardTitle}>Email</h2>
        </div>
        <label className={dashboardStyles.setting}>
          <span className={dashboardStyles.settingText}>
            <span className={dashboardStyles.settingLabel}>Email me my notifications</span>
            <span className={dashboardStyles.meta}>
              Also send the notifications below to your email. Security emails, like password changes, always send.
            </span>
          </span>
          <input
            type='checkbox'
            role='switch'
            className={dashboardStyles.switch}
            checked={values.emailEnabled}
            disabled={saving}
            onChange={(event) => save({emailEnabled: event.target.checked})}
          />
        </label>
      </section>

      <section className={dashboardStyles.card} aria-labelledby='notification-types-title'>
        <div className={dashboardStyles.cardHeader}>
          <h2 id='notification-types-title' className={dashboardStyles.cardTitle}>Notify me about</h2>
        </div>
        <p className={dashboardStyles.meta}>Turning a type off hides it from your inbox and your email.</p>

        {CATEGORIES.map(({field, label, description}) => (
          <label key={field} className={dashboardStyles.setting}>
            <span className={dashboardStyles.settingText}>
              <span className={dashboardStyles.settingLabel}>{label}</span>
              <span className={dashboardStyles.meta}>{description}</span>
            </span>
            <input
              type='checkbox'
              role='switch'
              className={dashboardStyles.switch}
              checked={values[field]}
              disabled={saving}
              onChange={(event) => save({[field]: event.target.checked})}
            />
          </label>
        ))}

        <label className={dashboardStyles.setting}>
          <span className={dashboardStyles.settingText}>
            <span className={dashboardStyles.settingLabel}>Remind me before meetings</span>
            <span className={dashboardStyles.meta}>
              {values.meetingReminders ? 'How early meeting reminders arrive.' : 'Turn on meeting reminders to choose.'}
            </span>
          </span>
          <select
            className={styles.leadSelect}
            value={values.reminderLeadMinutes}
            disabled={saving || !values.meetingReminders}
            onChange={(event) => save({reminderLeadMinutes: Number(event.target.value)})}
          >
            {leadTimes.map(({minutes, label}) => <option key={minutes} value={minutes}>{label}</option>)}
          </select>
        </label>

        {error && <p className={authStyles.error} role='alert'>{error}</p>}
      </section>
    </>
  );
}
