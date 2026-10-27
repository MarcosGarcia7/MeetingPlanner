import {headers} from 'next/headers';
import {notFound, redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {GroupBanResponse} from '@/lib/types/group';
import type {JoinRequestResponse} from '@/lib/types/request';
import type {UserProfileResponse} from '@/lib/types/user';
import {loadGroup, roleIn} from '@/app/components/dashboard/groups/load-group';
import GroupSettingsForm from '@/app/components/dashboard/groups/group-settings-form';
import GroupInviteForm from '@/app/components/dashboard/groups/group-invite-form';
import JoinRequestList from '@/app/components/dashboard/groups/join-request-list';
import BanList from '@/app/components/dashboard/groups/ban-list';
import DeleteGroup from '@/app/components/dashboard/groups/delete-group';

// Manage tab, for admins and the owner: settings, invites, join requests, bans, and (owner) delete.
// Member actions (roles, remove, ban, transfer) are on the Members tab.
export default async function GroupManagePage({params}: PageProps<'/dashboard/groups/[groupId]/manage'>) {
  const {userId} = await verifySession();
  const {groupId} = await params;
  const group = await loadGroup(groupId);
  const role = roleIn(group, userId);
  if (role === null) {
    return null;
  }
  if (role === 'MEMBER') {
    notFound();
  }
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);
  const id = Number(groupId);

  const results = await Promise.allSettled([
    get<CursorPage<JoinRequestResponse>>(`/api/groups/${id}/join-requests?status=PENDING`),
    get<CursorPage<GroupBanResponse>>(`/api/groups/${id}/bans`),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }
  const timeZone = valueOrNull(results[2])?.timezone ?? 'UTC';

  return (
    <>
      <GroupSettingsForm group={group} role={role}/>
      <GroupInviteForm groupId={id}/>
      <JoinRequestList groupId={id} initial={valueOrNull(results[0])} timeZone={timeZone}/>
      <BanList groupId={id} initial={valueOrNull(results[1])} role={role} timeZone={timeZone}/>
      {role === 'OWNER' && <DeleteGroup groupId={id} groupName={group.groupName ?? ''}/>}
    </>
  );
}
