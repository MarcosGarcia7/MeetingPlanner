'use client';

import {useEffect, useState} from 'react';
import {useForm, useWatch} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import type {MeetingDraftCheckResponse, MeetingResponse, MeetingUpdateRequest, TimeChangeCheckRequest}
  from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import {isoToZonedInput, zonedTimeToIso} from '@/app/components/dashboard/home/format';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

const editSchema = z.object({
  title: z.string().trim().min(1, 'Give the meeting a title').max(100, 'Title cannot exceed 100 characters'),
  start: z.string().min(1, 'Pick a start time'),
  end: z.string(),
  location: z.string().trim().max(500, 'Location cannot exceed 500 characters'),
  isPrivate: z.boolean(),
}).refine((data) => !data.end || data.end > data.start, {path: ['end'], message: 'End must be after the start'});
type EditFormData = z.infer<typeof editSchema>;

// How long the times must stay unchanged before they're checked for clashes.
const CHECK_DEBOUNCE_MS = 400;

// What the live check found for a new time: the editor's own clashes (saving would be rejected), the
// editor's availability, and other participants' clashes where the editor may see their schedule.
interface TimeCheck {
  conflicts: string[];
  warnings: string[];
}

const NO_CHECK: TimeCheck = {conflicts: [], warnings: []};

function toFormData(meeting: MeetingResponse, timeZone: string): EditFormData {
  return {
    title: meeting.title ?? '',
    start: isoToZonedInput(meeting.startTime!, timeZone),
    end: meeting.endTime ? isoToZonedInput(meeting.endTime, timeZone) : '',
    location: meeting.location ?? '',
    isPrivate: meeting.isPrivate,
  };
}

interface MeetingEditFormProps {
  meeting: MeetingResponse;
  viewerId: number;
  timeZone: string;
}

// Edits a meeting, sending only what changed. With other participants, a new time isn't applied directly:
// it becomes a proposal they vote on (one at a time), shown in the banner above the tabs.
export default function MeetingEditForm({meeting, viewerId, timeZone}: MeetingEditFormProps) {
  const router = useRouter();
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const othersComing = (meeting.participants ?? []).some((p) => p.userId !== viewerId);
  const {register, handleSubmit, reset, control, formState: {errors, isSubmitting, isDirty, dirtyFields}} =
    useForm<EditFormData>({resolver: zodResolver(editSchema), defaultValues: toFormData(meeting, timeZone)});
  const start = useWatch({control, name: 'start'});
  const end = useWatch({control, name: 'end'});
  const [check, setCheck] = useState<TimeCheck>(NO_CHECK);
  // Only a changed time that could be scheduled is checked; anything else is the form's own error.
  const timesDirty = Boolean(dirtyFields.start || dirtyFields.end);
  const checkable = timesDirty && Boolean(start) && (!end || end > start);

  useEffect(() => {
    if (!checkable) {
      return;
    }
    let stale = false;
    const timer = setTimeout(async () => {
      const body: TimeChangeCheckRequest = {
        startTime: zonedTimeToIso(start, timeZone),
        endTime: end ? zonedTimeToIso(end, timeZone) : undefined,
      };
      const result = await sendJson<MeetingDraftCheckResponse>(
        `/api/meetings/${meeting.id}/time-change/check`, 'POST', body);
      if (stale) {
        return;
      }
      if (!result.ok) {
        if (result.unauthenticated) {
          router.push('/login');
        }
        // A failed check (e.g. a start in the past) just shows nothing; saving reports the real error.
        setCheck(NO_CHECK);
        return;
      }
      setCheck({
        conflicts: result.data.conflicts ?? [],
        warnings: [
          ...(result.data.warnings ?? []),
          ...(result.data.recipients ?? []).flatMap((recipient) =>
            (recipient.warnings ?? []).map((warning) => `@${recipient.username}: ${warning}`)),
        ],
      });
    }, CHECK_DEBOUNCE_MS);
    return () => {
      stale = true;
      clearTimeout(timer);
    };
  }, [checkable, start, end, meeting.id, timeZone, router]);

  // Results for times that are no longer checkable (or no longer changed) are hidden, not cleared.
  const shown = checkable ? check : NO_CHECK;

  const onSubmit = async (data: EditFormData) => {
    setStatus(null);
    setError(null);
    const timesChanged = Boolean(dirtyFields.start || dirtyFields.end);
    // The API reads a missing end as "unchanged", so an end time can be moved but not removed.
    if (dirtyFields.end && !data.end && meeting.endTime) {
      setError('An end time can’t be removed once set. Pick a new end time instead.');
      return;
    }
    const body: MeetingUpdateRequest = {
      title: dirtyFields.title ? data.title : undefined,
      location: dirtyFields.location ? data.location : undefined,
      isPrivate: dirtyFields.isPrivate ? data.isPrivate : undefined,
      // Both times go together so the API checks the pair (end after start).
      startTime: timesChanged ? zonedTimeToIso(data.start, timeZone) : undefined,
      endTime: timesChanged && data.end ? zonedTimeToIso(data.end, timeZone) : undefined,
    };
    const result = await sendJson<MeetingResponse>(`/api/meetings/${meeting.id}`, 'PATCH', body);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    const proposed = timesChanged && result.data.pendingTimeChange !== null;
    // A proposed time leaves the meeting's own time as it was, so the form goes back to it.
    reset(toFormData(result.data, timeZone));
    setStatus(proposed ? 'Saved. The new time was sent to the other participants to approve.' : 'Changes saved.');
    router.refresh();
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='edit-meeting-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='edit-meeting-title' className={dashboardStyles.cardTitle}>Edit meeting</h2>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className={authStyles.form} noValidate>
        <label className={authStyles.field}>
          <span className={authStyles.label}>Title</span>
          <input type='text' maxLength={100} {...register('title')} className={authStyles.input}/>
          {errors.title && <span className={authStyles.error} role='alert'>{errors.title.message}</span>}
        </label>
        <div className={styles.formRow}>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Starts</span>
            <input type='datetime-local' disabled={meeting.pendingTimeChange !== null} {...register('start')}
                   className={authStyles.input}/>
            {errors.start && <span className={authStyles.error} role='alert'>{errors.start.message}</span>}
          </label>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Ends (optional)</span>
            <input type='datetime-local' disabled={meeting.pendingTimeChange !== null} {...register('end')}
                   className={authStyles.input}/>
            {errors.end && <span className={authStyles.error} role='alert'>{errors.end.message}</span>}
          </label>
        </div>
        <p className={dashboardStyles.meta}>
          Times are in {timeZone.replaceAll('_', ' ')}.{' '}
          {meeting.pendingTimeChange !== null
            ? 'A time change is already waiting on votes; the time can be changed again once it’s settled.'
            : othersComing ? 'Changing the time asks the other participants to approve it first.' : ''}
        </p>
        {(shown.conflicts.length > 0 || shown.warnings.length > 0) && (
          <div className={styles.notes} role='status'>
            {shown.conflicts.map((conflict) => <p key={conflict} className={authStyles.error}>{conflict}</p>)}
            {shown.warnings.map((warning) => <p key={warning} className={dashboardStyles.meta}>{warning}</p>)}
          </div>
        )}
        <label className={authStyles.field}>
          <span className={authStyles.label}>Location (optional)</span>
          <input type='text' maxLength={500} {...register('location')} className={authStyles.input}/>
          {errors.location && <span className={authStyles.error} role='alert'>{errors.location.message}</span>}
        </label>
        <label className={styles.checkboxField}>
          <input type='checkbox' {...register('isPrivate')}/>
          {meeting.groupId
            ? 'Private: only participants and group admins can see it'
            : 'Private: only participants can see it'}
        </label>
        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          {status && !isDirty && <span className={styles.status} role='status'>{status}</span>}
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={!isDirty || isSubmitting}
                  onClick={() => {
                    reset();
                    setError(null);
                  }}>
            Cancel
          </button>
          <button type='submit' className={dashboardStyles.button}
                  disabled={!isDirty || isSubmitting || shown.conflicts.length > 0}>
            {isSubmitting ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </section>
  );
}
