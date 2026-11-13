import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {InviteResponse} from '@/lib/types/invite';
import type {UserProfileResponse} from '@/lib/types/user';
import ActionList from '@/app/components/dashboard/inbox/action-list';
import {InviteHistory} from '@/app/components/dashboard/inbox/history';
import {inviteItem} from '@/app/components/dashboard/inbox/items';
import {profileTimeZone} from '@/lib/timezone';

export default async function InboxInvitesPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<InviteResponse>>('/api/invites?status=PENDING&size=50'),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const invites = valueOrNull(results[0]);
  const timeZone = profileTimeZone(valueOrNull(results[1]));

  return (
    <>
      <ActionList title='Pending invites' items={invites?.items.map((i) => inviteItem(i, timeZone)) ?? null}
                  empty='No invites waiting for you.' truncated={Boolean(invites?.nextCursor)}/>
      <InviteHistory/>
    </>
  );
}
