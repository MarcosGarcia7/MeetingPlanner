'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import type {InviteBatchResponse} from '@/lib/types/invite';
import type {MeetingCreateRequest, MeetingInviteBatchRequest, MeetingResponse} from '@/lib/types/meeting';
import {sendJson} from '@/lib/send-json';
import {zonedTimeToIso} from '@/app/components/dashboard/home/format';
import InviteeInput, {toInviteRequest} from './invitee-input';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

const meetingSchema = z.object({
  title: z.string().trim().min(1, 'Give the meeting a title').max(100, 'Title cannot exceed 100 characters'),
  start: z.string().min(1, 'Pick a start time'),
  end: z.string(),
  location: z.string().trim().max(500, 'Location cannot exceed 500 characters'),
  isPrivate: z.boolean(),
}).refine((data) => !data.end || data.end > data.start, {path: ['end'], message: 'End must be after the start'});
type MeetingFormData = z.infer<typeof meetingSchema>;

// What happened to the invites sent after the meeting was created; null when nobody was invited.
export interface InviteOutcome {
  invited: number;
  failed: string[];
  // Heads-ups about invitees, e.g. a clash with their schedule.
  warnings: string[];
}

function describe(failure: { username: string | null; email: string | null; reason: string | null }) {
  const who = failure.username ? `@${failure.username}` : failure.email;
  return `${who}: ${failure.reason ?? 'not invited'}`;
}

interface MeetingFormProps {
  groupId: number;
  // Profile timezone: the times typed in are read in it, matching how the dashboard displays them.
  timeZone: string;
  // Usernames suggested in the invite field (the group's other members).
  memberUsernames: string[];
  onCreated: (meeting: MeetingResponse, invites: InviteOutcome | null) => void;
  onCancel: () => void;
}

// Schedules a meeting in a group, then invites the chosen people in a second request. The API rejects
// times in the past and clashes with the creator's schedule (409); both come back as messages shown here.
// Once the meeting exists, invite failures are reported to the caller rather than keeping the form open,
// so a retry can't create the meeting twice.
export default function MeetingForm({groupId, timeZone, memberUsernames, onCreated, onCancel}: MeetingFormProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [invitees, setInvitees] = useState<string[]>([]);
  const {register, handleSubmit, formState: {errors, isSubmitting}} = useForm<MeetingFormData>({
    resolver: zodResolver(meetingSchema),
    defaultValues: {title: '', start: '', end: '', location: '', isPrivate: false},
  });

  const onSubmit = async (data: MeetingFormData) => {
    setError(null);
    const body: MeetingCreateRequest = {
      title: data.title,
      startTime: zonedTimeToIso(data.start, timeZone),
      endTime: data.end ? zonedTimeToIso(data.end, timeZone) : undefined,
      location: data.location || undefined,
      isPrivate: data.isPrivate,
    };
    const result = await sendJson<MeetingResponse>(`/api/groups/${groupId}/meetings`, 'POST', body);
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
      <label className={authStyles.field}>
        <span className={authStyles.label}>Location (optional)</span>
        <input type='text' maxLength={500} {...register('location')} className={authStyles.input}/>
        {errors.location && <span className={authStyles.error} role='alert'>{errors.location.message}</span>}
      </label>
      <InviteeInput value={invitees} onChange={setInvitees} suggestions={memberUsernames} disabled={isSubmitting}/>
      <label className={styles.checkboxField}>
        <input type='checkbox' {...register('isPrivate')}/>
        Private: only participants and group admins can see it
      </label>
      {error && <p className={authStyles.error} role='alert'>{error}</p>}
      <div className={styles.formFooter}>
        <button type='button' className={dashboardStyles.buttonSecondary} onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </button>
        <button type='submit' className={dashboardStyles.button} disabled={isSubmitting}>
          {isSubmitting ? 'Scheduling…' : invitees.length > 0 ? 'Schedule and invite' : 'Schedule meeting'}
        </button>
      </div>
    </form>
  );
}
