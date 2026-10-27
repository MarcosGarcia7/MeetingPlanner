import 'server-only';
import {cache} from 'react';
import {headers} from 'next/headers';
import {notFound, redirect} from 'next/navigation';
import {ApiError, AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {GroupResponse} from '@/lib/types/group';

export type GroupRole = 'OWNER' | 'ADMIN' | 'MEMBER';

// The group for /dashboard/groups/[groupId]. Cached per request, so the layout and the tab page share
// one API call. Unknown or malformed ids render the 404 page.
export const loadGroup = cache(async (groupId: string): Promise<GroupResponse> => {
  try {
    return await authFetch<GroupResponse>(`/api/groups/${encodeURIComponent(groupId)}`, {},
      getClientIp(await headers()));
  } catch (error) {
    if (error instanceof AuthError) {
      redirect('/login');
    }
    if (error instanceof ApiError && (error.status === 400 || error.status === 404)) {
      notFound();
    }
    throw error;
  }
});

// The viewer's role in the group, or null when they aren't a member (members is empty for non-members).
export function roleIn(group: GroupResponse, userId: number): GroupRole | null {
  if (group.ownerId === userId) {
    return 'OWNER';
  }
  const member = group.members?.find((m) => m.userId === userId);
  if (!member) {
    return null;
  }
  return member.isAdmin ? 'ADMIN' : 'MEMBER';
}
