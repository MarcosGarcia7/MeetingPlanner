import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {InviteResponse} from '@/lib/types/invite';
import ActionList from '@/app/components/dashboard/inbox/action-list';
import {InviteHistory} from '@/app/components/dashboard/inbox/history';
import {inviteItem} from '@/app/components/dashboard/inbox/items';

export default async function InboxInvitesPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());

  const results = await Promise.allSettled([
    authFetch<CursorPage<InviteResponse>>('/api/invites?status=PENDING&size=50', {}, clientIp),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const invites = valueOrNull(results[0]);

  return (
    <>
      <ActionList title='Pending invites' items={invites?.items.map(inviteItem) ?? null}
                  empty='No invites waiting for you.' truncated={Boolean(invites?.nextCursor)}/>
      <InviteHistory/>
    </>
  );
}
