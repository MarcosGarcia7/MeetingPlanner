import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {InviteResponse} from '@/lib/types/invite';
import type {TimeChangeProposalResponse} from '@/lib/types/meeting';
import type {NotificationResponse} from '@/lib/types/notification';
import type {JoinRequestResponse} from '@/lib/types/request';
import type {UserProfileResponse} from '@/lib/types/user';
import ActivityFeed from '@/app/components/dashboard/inbox/activity-feed';
import styles from '@/app/components/dashboard/inbox/inbox.module.css';

const COUNT_SIZE = 50;

// "12", "50+" (more than one page), or "–" when the count couldn't be loaded.
function countLabel(...pages: ({ items: unknown[]; nextCursor?: string | null } | null)[]): string {
  if (pages.some((page) => page === null)) {
    return '–';
  }
  const total = pages.reduce((sum, page) => sum + page!.items.length, 0);
  return pages.some((page) => page!.nextCursor) ? `${total}+` : String(total);
}

export default async function InboxOverviewPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<CursorPage<InviteResponse>>(`/api/invites?status=PENDING&size=${COUNT_SIZE}`),
    get<CursorPage<FriendshipResponse>>(`/api/users/me/friends/requests/incoming?size=${COUNT_SIZE}`),
    get<CursorPage<JoinRequestResponse>>(`/api/requests/incoming?status=PENDING&size=${COUNT_SIZE}`),
    get<TimeChangeProposalResponse[]>('/api/meetings/time-changes/pending'),
    get<CursorPage<NotificationResponse>>('/api/users/me/notifications'),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const votes = valueOrNull(results[3]);
  const tiles = [
    {href: '/dashboard/inbox/invites', count: countLabel(valueOrNull(results[0])), label: 'Invites to answer'},
    {
      href: '/dashboard/inbox/requests',
      count: countLabel(valueOrNull(results[1]), valueOrNull(results[2])),
      label: 'Requests to review',
    },
    {href: '/dashboard/inbox/proposals', count: countLabel(votes && {items: votes}), label: 'Time changes to vote on'},
  ];

  return (
    <>
      <nav className={styles.summary} aria-label='Pending items'>
        {tiles.map((tile) => (
          <Link key={tile.href} href={tile.href} className={styles.summaryTile}>
            <span className={styles.summaryCount}>{tile.count}</span>
            <span className={styles.summaryLabel}>{tile.label}</span>
          </Link>
        ))}
      </nav>
      <ActivityFeed initial={valueOrNull(results[4])} timeZone={valueOrNull(results[5])?.timezone ?? 'UTC'}/>
    </>
  );
}
