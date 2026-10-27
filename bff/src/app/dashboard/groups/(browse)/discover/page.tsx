import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {GroupSummaryResponse} from '@/lib/types/group';
import GroupSearch from '@/app/components/dashboard/groups/group-search';
import GroupShelf from '@/app/components/dashboard/groups/group-shelf';
import styles from '@/app/components/dashboard/groups/groups.module.css';

export default async function DiscoverGroupsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<GroupSummaryResponse[]>('/api/groups/suggested'),
    get<GroupSummaryResponse[]>('/api/groups/popular'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const suggested = valueOrNull(results[0]);
  const popular = valueOrNull(results[1]);

  return (
    <>
      <GroupSearch/>
      <div className={styles.shelves}>
        {/* Hidden when empty: with no friends in other groups, Popular is the main shelf. */}
        {suggested?.length !== 0 && (
          <GroupShelf title='Your friends are in' groups={suggested}
                      empty='None of your friends are in groups you could join.'/>
        )}
        <GroupShelf title='Popular groups' groups={popular} empty='No other groups to join yet.'/>
      </div>
    </>
  );
}
