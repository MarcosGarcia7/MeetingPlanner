import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {CursorPage} from '@/lib/types/api';
import type {GroupSummaryResponse} from '@/lib/types/group';
import type {InviteResponse} from '@/lib/types/invite';
import type {JoinRequestResponse} from '@/lib/types/request';
import GroupSearch from '@/app/components/dashboard/groups/group-search';
import GroupShelf from '@/app/components/dashboard/groups/group-shelf';
import PendingGroupItems from '@/app/components/dashboard/groups/pending-group-items';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/groups/groups.module.css';

// Each shelf gets its own data or null, so one failing call degrades that shelf instead of the page.
function valueOrNull<T>(result: PromiseSettledResult<T>): T | null {
  return result.status === 'fulfilled' ? result.value : null;
}

export default async function GroupsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<InviteResponse>>('/api/invites?status=PENDING&kind=GROUP&size=50'),
    get<CursorPage<JoinRequestResponse>>('/api/requests?status=PENDING&kind=GROUP&size=50'),
    get<CursorPage<GroupSummaryResponse>>('/api/groups?size=50'),
    get<GroupSummaryResponse[]>('/api/groups/suggested'),
    get<GroupSummaryResponse[]>('/api/groups/popular'),
  ] as const);

  if (results.some((result) => result.status === 'rejected' && result.reason instanceof AuthError)) {
    redirect('/login');
  }

  const invites = valueOrNull(results[0])?.items ?? [];
  const requests = valueOrNull(results[1])?.items ?? [];
  const mine = valueOrNull(results[2]);
  const suggested = valueOrNull(results[3]);
  const popular = valueOrNull(results[4]);

  return (
    <main className={dashboardStyles.page}>
      {(invites.length > 0 || requests.length > 0) && (
        <PendingGroupItems invites={invites} requests={requests}/>
      )}
      <GroupSearch/>
      <div className={styles.shelves}>
        <GroupShelf title='My groups' groups={mine?.items ?? null}
                    empty='You’re not in any groups yet. Join one below or search by name.'/>
        {/* Hidden when empty: with no friends in other groups, Popular is the main shelf. */}
        {suggested?.length !== 0 && (
          <GroupShelf title='Your friends are in' groups={suggested}
                      empty='None of your friends are in groups you could join.'/>
        )}
        <GroupShelf title='Popular groups' groups={popular} empty='No other groups to join yet.'/>
      </div>
    </main>
  );
}
