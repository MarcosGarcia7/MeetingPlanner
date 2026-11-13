import type {TargetKind} from '@/lib/types/api';

// Where a share-link member lands: the meeting or the group in the dashboard. Shared by the server page
// (redirecting existing members) and the client actions (after joining).
export function shareLinkDashboardPath(kind: TargetKind, groupId: number | null, meetingId: number | null): string {
  if (kind === 'MEETING' && meetingId != null) return `/dashboard/schedule/${meetingId}`;
  if (groupId != null) return `/dashboard/groups/${groupId}`;
  return '/dashboard';
}
