'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {XMarkIcon} from '@heroicons/react/24/solid';
import type {UserAvailabilityResponse, UserAvailabilityUpdateRequest} from '@/lib/types/user';
import {sendJson} from '@/lib/send-json';
import {
  DAY_END, DAY_LABELS, DAYS, type Day, fromTimeInput, sortWindows, toTimeInput, toWindows, WEEKDAYS,
  windowError,
} from './availability';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './schedule.module.css';

// What the time inputs hold while editing; either side may be empty mid-edit.
interface DraftWindow {
  start: string;
  end: string;
}

type Draft = Record<Day, DraftWindow[]>;

function toDraft(availability: UserAvailabilityResponse | null): Draft {
  const windows = toWindows(availability);
  return Object.fromEntries(DAYS.map((day) => [day, sortWindows(windows[day]).map(([start, end]) => ({
    start: toTimeInput(start),
    end: toTimeInput(end),
  }))])) as Draft;
}

// Parsed windows, or an error message when a day can't be saved as it stands.
function parseDay(windows: DraftWindow[]): { windows: number[][] } | { error: string } {
  const parsed: number[][] = [];
  for (const window of windows) {
    const start = fromTimeInput(window.start, false);
    const end = fromTimeInput(window.end, true);
    if (start === null || end === null) {
      return {error: 'Fill in both times.'};
    }
    parsed.push([start, end]);
  }
  const error = windowError(parsed);
  return error ? {error} : {windows: sortWindows(parsed)};
}

// A new window starts where the day's last one ends (an hour long), or 9–5 on an empty day.
function nextWindow(windows: DraftWindow[]): DraftWindow {
  const lastEnd = Math.max(0, ...windows.map((window) => fromTimeInput(window.end, true) ?? 0));
  if (lastEnd === 0) {
    return {start: '09:00', end: '17:00'};
  }
  const start = Math.min(lastEnd, DAY_END - 60);
  return {start: toTimeInput(start), end: toTimeInput(Math.min(start + 60, DAY_END))};
}

interface AvailabilityFormProps {
  availability: UserAvailabilityResponse | null;
  timeZone: string;
}

// The weekly hours someone is usually free, as time ranges per day. With "Use my availability" on,
// scheduling a meeting with them outside these hours comes with a warning.
export default function AvailabilityForm({availability, timeZone}: AvailabilityFormProps) {
  const router = useRouter();
  const [saved, setSaved] = useState(() => ({draft: toDraft(availability), enabled: availability?.availabilitySet ?? false}));
  const [draft, setDraft] = useState(saved.draft);
  const [enabled, setEnabled] = useState(saved.enabled);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const parsed = Object.fromEntries(DAYS.map((day) => [day, parseDay(draft[day])])) as
    Record<Day, ReturnType<typeof parseDay>>;
  const invalid = DAYS.some((day) => 'error' in parsed[day]);
  const dirty = enabled !== saved.enabled || JSON.stringify(draft) !== JSON.stringify(saved.draft);

  function edit(day: Day, windows: DraftWindow[]) {
    setStatus(null);
    setDraft((current) => ({...current, [day]: windows}));
  }

  function copyMondayToWeekdays() {
    setStatus(null);
    setDraft((current) => ({
      ...current,
      ...Object.fromEntries(WEEKDAYS.map((day) => [day, current.monday.map((window) => ({...window}))])),
    }));
  }

  async function save() {
    if (invalid) {
      return;
    }
    setSaving(true);
    setStatus(null);
    setError(null);
    const body: UserAvailabilityUpdateRequest = {
      ...Object.fromEntries(DAYS.map((day) => {
        const result = parsed[day];
        return [day, 'windows' in result ? result.windows : []];
      })),
      availabilitySet: enabled,
    };
    const result = await sendJson<UserAvailabilityResponse>('/api/users/me/availability', 'PUT', body);
    setSaving(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    const next = {draft: toDraft(result.data), enabled: result.data.availabilitySet};
    setSaved(next);
    setDraft(next.draft);
    setEnabled(next.enabled);
    setStatus('Availability saved.');
    router.refresh();
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='availability-title'>
      <div className={styles.cardHeader}>
        <h2 id='availability-title' className={dashboardStyles.cardTitle}>Weekly availability</h2>
        <span className={dashboardStyles.meta}>Times are in {timeZone.replaceAll('_', ' ')}.</span>
      </div>

      <label className={dashboardStyles.setting}>
        <span className={dashboardStyles.settingText}>
          <span className={dashboardStyles.settingLabel}>Use my availability</span>
          <span className={dashboardStyles.meta}>
            {enabled
              ? 'People scheduling with you are warned when a meeting falls outside these hours.'
              : 'Your hours are saved but not used. Turn this on to get warnings about meetings outside them.'}
          </span>
        </span>
        <input type='checkbox' role='switch' className={dashboardStyles.switch} checked={enabled} disabled={saving}
               onChange={(event) => {
                 setStatus(null);
                 setEnabled(event.target.checked);
               }}/>
      </label>

      <ul className={styles.days}>
        {DAYS.map((day) => {
          const windows = draft[day];
          const result = parsed[day];
          return (
            <li key={day} className={styles.day}>
              <span className={styles.dayName}>{DAY_LABELS[day]}</span>
              <div className={styles.windows}>
                {windows.length === 0 && <span className={styles.unavailable}>Unavailable</span>}
                {windows.map((window, index) => (
                  <div key={index} className={styles.window}>
                    <input type='time' value={window.start} disabled={saving} className={authStyles.input}
                           aria-label={`${DAY_LABELS[day]} window ${index + 1} start`}
                           onChange={(event) => edit(day, windows.map((w, i) =>
                             i === index ? {...w, start: event.target.value} : w))}/>
                    <span className={dashboardStyles.meta} aria-hidden>–</span>
                    <input type='time' value={window.end} disabled={saving} className={authStyles.input}
                           aria-label={`${DAY_LABELS[day]} window ${index + 1} end`}
                           onChange={(event) => edit(day, windows.map((w, i) =>
                             i === index ? {...w, end: event.target.value} : w))}/>
                    <button type='button' className={styles.iconButton} disabled={saving}
                            aria-label={`Remove ${DAY_LABELS[day]} window ${index + 1}`}
                            onClick={() => edit(day, windows.filter((_, i) => i !== index))}>
                      <XMarkIcon className={styles.icon} aria-hidden/>
                    </button>
                  </div>
                ))}
                {'error' in result && <span className={authStyles.error} role='alert'>{result.error}</span>}
              </div>
              <button type='button' className={dashboardStyles.buttonSecondary} disabled={saving}
                      onClick={() => edit(day, [...windows, nextWindow(windows)])}>
                Add hours
              </button>
            </li>
          );
        })}
      </ul>
      <p className={dashboardStyles.meta}>An end time of 12:00 AM means the end of the day.</p>

      {error && <p className={authStyles.error} role='alert'>{error}</p>}
      <div className={styles.footer}>
        <button type='button' className={dashboardStyles.buttonSecondary} disabled={saving}
                onClick={copyMondayToWeekdays}>
          Copy Monday to weekdays
        </button>
        <span className={styles.status} role='status'>{status && !dirty ? status : null}</span>
        <button type='button' className={dashboardStyles.buttonSecondary} disabled={!dirty || saving}
                onClick={() => {
                  setDraft(saved.draft);
                  setEnabled(saved.enabled);
                  setError(null);
                }}>
          Cancel
        </button>
        <button type='button' className={dashboardStyles.button} disabled={!dirty || invalid || saving}
                onClick={save}>
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </div>
    </section>
  );
}
