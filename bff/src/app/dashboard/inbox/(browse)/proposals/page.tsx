import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {TimeChangeProposalResponse} from '@/lib/types/meeting';
import type {UserProfileResponse} from '@/lib/types/user';
import ActionList from '@/app/components/dashboard/inbox/action-list';
import {ProposalHistory} from '@/app/components/dashboard/inbox/history';
import {ownProposalItem, voteItem} from '@/app/components/dashboard/inbox/items';

export default async function InboxProposalsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<TimeChangeProposalResponse[]>('/api/meetings/time-changes/pending'),
    get<CursorPage<TimeChangeProposalResponse>>('/api/meetings/time-changes?role=PROPOSER&status=PENDING&size=50'),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const toVote = valueOrNull(results[0]);
  const mine = valueOrNull(results[1]);
  const timeZone = valueOrNull(results[2])?.timezone ?? 'UTC';

  return (
    <>
      <ActionList title='Time changes to vote on' items={toVote?.map((p) => voteItem(p, timeZone)) ?? null}
                  empty='No time changes waiting for your vote.'/>
      <ActionList title='Your proposals' items={mine?.items.map((p) => ownProposalItem(p, timeZone)) ?? null}
                  empty='You have no time changes waiting on votes.' truncated={Boolean(mine?.nextCursor)}/>
      <ProposalHistory timeZone={timeZone}/>
    </>
  );
}
