import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import UserSearch from '@/app/components/dashboard/friends/user-search';
import FriendList from '@/app/components/dashboard/friends/friend-list';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/friends/friends.module.css';
import {incomingCountLabel} from './incoming-count';
import {pageKey} from './page-key';

export default async function FriendsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends'),
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends/requests/incoming'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const friends = valueOrNull(results[0]);
  const incoming = valueOrNull(results[1]);
  const incomingCount = incoming ? incomingCountLabel(incoming) : null;

  return (
    <>
      <UserSearch/>
      {incomingCount && (
        <div className={styles.notice}>
          <span>
            You have {incomingCount} friend {incomingCount === '1' ? 'request' : 'requests'} waiting.
          </span>
          <Link href='/dashboard/friends/requests' className={dashboardStyles.cardLink}>Review</Link>
        </div>
      )}
      {/* Remounts when a refresh changes the first page (e.g. accepting from search), since the list
          only reads `initial` on mount. */}
      <FriendList key={pageKey(friends)} initial={friends}/>
    </>
  );
}
