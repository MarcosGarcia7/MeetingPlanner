'use client';

import {useEffect, useState} from 'react';
import {useForm, useWatch} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import type {InviteBatchResponse} from '@/lib/types/invite';
import type {MeetingCreateRequest, MeetingDraftCheckRequest, MeetingDraftCheckResponse, MeetingInviteBatchRequest,
  MeetingResponse} from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import {zonedTimeToIso} from '@/app/components/dashboard/home/format';
import InviteeInput, {toInviteRequest} from '@/app/components/dashboard/groups/invitee-input';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './meetings.module.css';

const meetingSchema = z.object({
  title: z.string().trim().min(1, 'Give the meeting a title').max(100, 'Title cannot exceed 100 characters'),
  // A group id, or '' for a meeting outside any group.
  groupId: z.string(),
  start: z.string().min(1, 'Pick a start time'),
  end: z.string(),
  location: z.string().trim().max(500, 'Location cannot exceed 500 characters'),
  isPrivate: z.boolean(),
}).refine((data) => !data.end || data.end > data.start, {path: ['end'], message: 'End must be after the start'});
type MeetingFormData = z.infer<typeof meetingSchema>;

// How long the times and invitees must stay unchanged before they're checked for clashes.
const CHECK_DEBOUNCE_MS = 400;

// What the live check found for the times and invitees currently on the form.
interface DraftCheck {
  // The creator's own clashes; the API would reject the meeting.
  conflicts: string[];
  // The creator's availability.
  warnings: string[];
  // Invitee clashes, for people whose schedule the creator may see.
  recipientWarnings: string[];
}

const NO_CHECK: DraftCheck = {conflicts: [], warnings: [], recipientWarnings: []};

// What happened to the invites sent after the meeting was created; null when nobody was invited.
export interface InviteOutcome {
  invited: number;
  failed: string[];
  // Heads-ups about invitees, e.g. a clash with their schedule.
  warnings: string[];
}

export interface GroupOption {
  id: number;
  name: string;
}

function who(entry: { username: string | null; email: string | null }) {
  return entry.username ? `@${entry.username}` : entry.email;
}

function describe(failure: { username: string | null; email: string | null; reason: string | null }) {
  return `${who(failure)}: ${failure.reason ?? 'not invited'}`;
}

interface MeetingFormProps {
  // Schedules in this group (a group's Meetings tab). Without it, the form offers `groups` to choose from,
  // defaulting to no group.
  groupId?: number;
  groups?: GroupOption[];
  // Profile timezone: the times typed in are read in it, matching how the dashboard displays them.
  timeZone: string;
  // Usernames suggested in the invite field (a group's other members, or friends).
  suggestions: string[];
  // Told when the chosen group changes (null: no group), e.g. to suggest that group's members instead.
  onGroupChange?: (groupId: number | null) => void;
  onCreated: (meeting: MeetingResponse, invites: InviteOutcome | null) => void;
  onCancel: () => void;
}

// Schedules a meeting, in a group or on its own, then invites the chosen people in a second request. While
// the form is filled in, the times and invitees are checked against schedules (POST /api/meetings/check)
// each time they change, so the creator's clashes block submitting and invitee clashes show as warnings
// before anything is created. The API still rejects times in the past, clashes with the creator's
// schedule (409), and groups where the creator may not schedule (403); each comes back as a message shown
// here. Once the meeting exists, invite failures are reported to the caller rather than keeping the form
// open, so a retry can't create the meeting twice.
export default function MeetingForm({groupId, groups = [], timeZone, suggestions, onGroupChange, onCreated,
                                      onCancel}: MeetingFormProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [invitees, setInvitees] = useState<string[]>([]);
  const {register, handleSubmit, control, formState: {errors, isSubmitting}} = useForm<MeetingFormData>({
    resolver: zodResolver(meetingSchema),
    defaultValues: {title: '', groupId: groupId ? String(groupId) : '', start: '', end: '', location: '',
      isPrivate: false},
  });
  const inGroup = useWatch({control, name: 'groupId'}) !== '';
  const start = useWatch({control, name: 'start'});
  const end = useWatch({control, name: 'end'});
  const [check, setCheck] = useState<DraftCheck>(NO_CHECK);
  // Only check times that could be scheduled; anything else is the form's own validation error.
  const checkable = Boolean(start) && (!end || end > start);

  useEffect(() => {
    if (!checkable) {
      return;
    }
    let stale = false;
    const timer = setTimeout(async () => {
      const body: MeetingDraftCheckRequest = {
        startTime: zonedTimeToIso(start, timeZone),
        endTime: end ? zonedTimeToIso(end, timeZone) : undefined,
        recipients: invitees.map(toInviteRequest),
      };
      const result = await sendJson<MeetingDraftCheckResponse>('/api/meetings/check', 'POST', body);
      if (stale) {
        return;
      }
      if (!result.ok) {
        if (result.unauthenticated) {
          router.push('/login');
        }
        // A failed check (e.g. a start in the past) just shows nothing; submitting reports the real error.
        setCheck(NO_CHECK);
        return;
      }
      setCheck({
        conflicts: result.data.conflicts ?? [],
        warnings: result.data.warnings ?? [],
        recipientWarnings: (result.data.recipients ?? []).flatMap((recipient) =>
          (recipient.warnings ?? []).map((warning) => `${who(recipient)}: ${warning}`)),
      });
    }, CHECK_DEBOUNCE_MS);
    return () => {
      stale = true;
      clearTimeout(timer);
    };
  }, [checkable, start, end, invitees, timeZone, router]);

  // Results for times that are no longer checkable are hidden rather than cleared from the effect.
  const shown = checkable ? check : NO_CHECK;

  const onSubmit = async (data: MeetingFormData) => {
    setError(null);
    const body: MeetingCreateRequest = {
      title: data.title,
      startTime: zonedTimeToIso(data.start, timeZone),
      endTime: data.end ? zonedTimeToIso(data.end, timeZone) : undefined,
      location: data.location || undefined,
      isPrivate: data.isPrivate,
    };
    const url = data.groupId ? `/api/groups/${data.groupId}/meetings` : '/api/meetings';
    const result = await sendJson<MeetingResponse>(url, 'POST', body);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    const meeting = result.data;
    if (invitees.length === 0) {
      onCreated(meeting, null);
      return;
    }

    const inviteBody: MeetingInviteBatchRequest = {recipients: invitees.map(toInviteRequest)};
    const invites = await sendJson<InviteBatchResponse>(`/api/meetings/${meeting.id}/invites`, 'POST', inviteBody);
    if (!invites.ok && invites.unauthenticated) {
      router.push('/login');
      return;
    }
    onCreated(meeting, invites.ok
      ? {
        invited: invites.data.invited.length,
        failed: invites.data.failed.map(describe),
        warnings: invites.data.invited.flatMap((invite) => invite.warnings),
      }
      // The whole batch failed (400 whose message lists each reason, or a network error).
      : {invited: 0, failed: [invites.message], warnings: []});
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className={authStyles.form} noValidate>
      <label className={authStyles.field}>
        <span className={authStyles.label}>Title</span>
        <input type='text' maxLength={100} autoFocus {...register('title')} className={authStyles.input}/>
        {errors.title && <span className={authStyles.error} role='alert'>{errors.title.message}</span>}
      </label>
      {!groupId && groups.length > 0 && (
        <label className={authStyles.field}>
          <span className={authStyles.label}>Group (optional)</span>
          <select {...register('groupId', {
            onChange: (event) => onGroupChange?.(event.target.value ? Number(event.target.value) : null),
          })} className={authStyles.input}>
            <option value=''>No group</option>
            {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
          </select>
        </label>
      )}
      <div className={styles.formRow}>
        <label className={authStyles.field}>
          <span className={authStyles.label}>Starts</span>
          <input type='datetime-local' {...register('start')} className={authStyles.input}/>
          {errors.start && <span className={authStyles.error} role='alert'>{errors.start.message}</span>}
        </label>
        <label className={authStyles.field}>
          <span className={authStyles.label}>Ends (optional)</span>
          <input type='datetime-local' {...register('end')} className={authStyles.input}/>
          {errors.end && <span className={authStyles.error} role='alert'>{errors.end.message}</span>}
        </label>
      </div>
      <p className={dashboardStyles.meta}>Times are in {timeZone.replaceAll('_', ' ')}.</p>
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
      <InviteeInput value={invitees} onChange={setInvitees} suggestions={suggestions} disabled={isSubmitting}/>
      {shown.recipientWarnings.length > 0 && (
        <div className={styles.notes} role='status'>
          {shown.recipientWarnings.map((warning) => <p key={warning} className={dashboardStyles.meta}>{warning}</p>)}
        </div>
      )}
      <label className={styles.checkboxField}>
        <input type='checkbox' {...register('isPrivate')}/>
        {inGroup
          ? 'Private: only participants and group admins can see it'
          : 'Private: only participants can see it'}
      </label>
      {error && <p className={authStyles.error} role='alert'>{error}</p>}
      <div className={styles.formFooter}>
        <button type='button' className={dashboardStyles.buttonSecondary} onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </button>
        <button type='submit' className={dashboardStyles.button}
                disabled={isSubmitting || shown.conflicts.length > 0}>
          {isSubmitting ? 'Scheduling…' : invitees.length > 0 ? 'Schedule and invite' : 'Schedule meeting'}
        </button>
      </div>
    </form>
  );
}
