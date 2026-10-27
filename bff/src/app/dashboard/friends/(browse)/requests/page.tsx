import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import RequestList from '@/app/components/dashboard/friends/request-list';
import {pageKey} from '../page-key';

export default async function FriendRequestsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends/requests/incoming'),
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends/requests/outgoing'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const incoming = valueOrNull(results[0]);
  const outgoing = valueOrNull(results[1]);

  return (
    <>
      <RequestList key={pageKey(incoming)} direction='incoming' initial={incoming}/>
      <RequestList key={pageKey(outgoing)} direction='outgoing' initial={outgoing}/>
    </>
  );
}
