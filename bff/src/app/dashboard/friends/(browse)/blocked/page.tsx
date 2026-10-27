import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import BlockedList from '@/app/components/dashboard/friends/blocked-list';
import {pageKey} from '../page-key';

export default async function BlockedPage() {
  await verifySession();
  const results = await Promise.allSettled([
    authFetch<CursorPage<FriendshipResponse>>('/api/users/me/friends/blocked', {}, getClientIp(await headers())),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const blocked = valueOrNull(results[0]);
  return <BlockedList key={pageKey(blocked)} initial={blocked}/>;
}
