import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {PostResponse} from '@/lib/types/group';
import type {UserProfileResponse} from '@/lib/types/user';
import {loadGroup, roleIn} from '@/app/components/dashboard/groups/load-group';
import GroupPosts from '@/app/components/dashboard/groups/group-posts';

// Posts tab. The layout already stopped non-members, so role is set here.
export default async function GroupPostsPage({params}: PageProps<'/dashboard/groups/[groupId]'>) {
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
    get<CursorPage<PostResponse>>(`/api/groups/${group.id}/posts`),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  return (
    <GroupPosts groupId={Number(groupId)} initial={valueOrNull(results[0])} viewerId={userId} role={role}
                canPost={role !== 'MEMBER' || group.membersCanPost}
                timeZone={valueOrNull(results[1])?.timezone ?? 'UTC'}/>
  );
}
