import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {JoinRequestResponse} from '@/lib/types/request';
import type {UserProfileResponse} from '@/lib/types/user';
import ActionList from '@/app/components/dashboard/inbox/action-list';
import {RequestHistory} from '@/app/components/dashboard/inbox/history';
import {
  incomingFriendItem,
  incomingRequestItem,
  outgoingFriendItem,
  sentRequestItem,
} from '@/app/components/dashboard/inbox/items';

export default async function InboxRequestsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends/requests/incoming?size=50'),
    get<CursorPage<JoinRequestResponse>>('/api/requests/incoming?status=PENDING&size=50'),
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends/requests/outgoing?size=50'),
    get<CursorPage<JoinRequestResponse>>('/api/requests?status=PENDING&size=50'),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const friendsIn = valueOrNull(results[0]);
  const joinIn = valueOrNull(results[1]);
  const friendsOut = valueOrNull(results[2]);
  const joinOut = valueOrNull(results[3]);
  const timeZone = valueOrNull(results[4])?.timezone ?? 'UTC';

  const incoming = friendsIn && joinIn
    ? [...friendsIn.items.map(incomingFriendItem), ...joinIn.items.map(incomingRequestItem)]
    : null;
  const sent = friendsOut && joinOut
    ? [...joinOut.items.map(sentRequestItem), ...friendsOut.items.map(outgoingFriendItem)]
    : null;

  return (
    <>
      <ActionList title='Waiting for you' items={incoming} empty='Nobody is waiting on you.'
                  truncated={Boolean(friendsIn?.nextCursor || joinIn?.nextCursor)}/>
      <ActionList title='Sent by you' items={sent} empty='You have no open requests.'
                  truncated={Boolean(friendsOut?.nextCursor || joinOut?.nextCursor)}/>
      <RequestHistory timeZone={timeZone}/>
    </>
  );
}
