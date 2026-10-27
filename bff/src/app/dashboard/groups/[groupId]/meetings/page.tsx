import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {MeetingSummaryResponse} from '@/lib/types/meeting';
import type {UserProfileResponse} from '@/lib/types/user';
import {loadGroup, roleIn} from '@/app/components/dashboard/groups/load-group';
import GroupMeetings from '@/app/components/dashboard/groups/group-meetings';

export default async function GroupMeetingsPage({params}: PageProps<'/dashboard/groups/[groupId]/meetings'>) {
  const {userId} = await verifySession();
  const {groupId} = await params;
  const group = await loadGroup(groupId);
  const role = roleIn(group, userId);
  if (!role) {
    return null;
  }
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<MeetingSummaryResponse>>(`/api/groups/${group.id}/meetings`),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  return (
    <GroupMeetings groupId={Number(groupId)} groupName={group.groupName} initial={valueOrNull(results[0])}
                   timeZone={valueOrNull(results[1])?.timezone ?? 'UTC'}
                   canCreate={role !== 'MEMBER' || group.membersCanCreate}
                   memberUsernames={(group.members ?? []).flatMap((member) =>
                     member.userId !== userId && member.username ? [member.username] : [])}/>
  );
}
