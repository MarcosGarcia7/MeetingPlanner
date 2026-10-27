import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {GroupSummaryResponse} from '@/lib/types/group';
import type {InviteResponse} from '@/lib/types/invite';
import type {JoinRequestResponse} from '@/lib/types/request';
import GroupShelf from '@/app/components/dashboard/groups/group-shelf';
import PendingGroupItems from '@/app/components/dashboard/groups/pending-group-items';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/groups/groups.module.css';

const PREVIEW_SIZE = 5;
const TEASER_SIZE = 3;

export default async function GroupsOverviewPage() {
  const {userId} = await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<InviteResponse>>('/api/invites?status=PENDING&kind=GROUP&size=50'),
    get<CursorPage<JoinRequestResponse>>('/api/requests?status=PENDING&kind=GROUP&size=50'),
    get<CursorPage<GroupSummaryResponse>>(`/api/groups?size=${PREVIEW_SIZE}`),
    get<GroupSummaryResponse[]>(`/api/groups/suggested?limit=${TEASER_SIZE}`),
    get<GroupSummaryResponse[]>(`/api/groups/popular?limit=${TEASER_SIZE}`),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const invites = valueOrNull(results[0])?.items ?? [];
  const requests = valueOrNull(results[1])?.items ?? [];
  const mine = valueOrNull(results[2]);
  const suggested = valueOrNull(results[3]);
  const popular = valueOrNull(results[4]);
  const showSuggested = suggested !== null && suggested.length > 0;

  return (
    <>
      {(invites.length > 0 || requests.length > 0) && (
        <PendingGroupItems invites={invites} requests={requests}/>
      )}
      <div className={styles.shelves}>
        <GroupShelf title='My groups' groups={mine?.items ?? null} viewerId={userId}
                    empty='You’re not in any groups yet.'
                    footer={
                      <div className={dashboardStyles.actions}>
                        {mine?.nextCursor && (
                          <Link href='/dashboard/groups/mine' className={dashboardStyles.cardLink}>See all</Link>
                        )}
                        <Link href='/dashboard/groups/mine?new=1' className={dashboardStyles.cardLink}>
                          Create a group
                        </Link>
                      </div>
                    }/>
        <GroupShelf title={showSuggested ? 'Your friends are in' : 'Popular groups'}
                    groups={showSuggested ? suggested : popular}
                    empty='No other groups to join yet.'
                    footer={<Link href='/dashboard/groups/discover' className={dashboardStyles.cardLink}>Discover more</Link>}/>
      </div>
    </>
  );
}
