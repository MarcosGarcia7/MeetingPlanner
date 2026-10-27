import type {MeetingResponse, MeetingSummaryResponse} from '@/lib/types/meeting';
import type {InviteOutcome} from './meeting-form';

// The list-row shape of a meeting just created (the create endpoint returns the full MeetingResponse).
export function toSummary(meeting: MeetingResponse): MeetingSummaryResponse {
  return {
    id: meeting.id!,
    groupId: meeting.groupId,
    groupName: meeting.groupName,
    creatorId: meeting.creatorId!,
    startTime: meeting.startTime!,
    endTime: meeting.endTime,
    location: meeting.location,
    title: meeting.title ?? '',
    isPrivate: meeting.isPrivate,
    participantCount: meeting.participants?.length ?? 1,
  };
}

// What to tell the creator after the form succeeds: the API's warnings and how its invites went.
export function creationNotes(meeting: MeetingResponse, invites: InviteOutcome | null): {
  notes: string[];
  failures: string[];
} {
  const created = invites && invites.invited > 0
    ? `Meeting scheduled. Invited ${invites.invited} ${invites.invited === 1 ? 'person' : 'people'}.`
    : 'Meeting scheduled.';
  return {
    notes: [created, ...(meeting.warnings ?? []), ...(invites?.warnings ?? [])],
    failures: invites?.failed.length ? ['Some invites weren’t sent:', ...invites.failed] : [],
  };
}
