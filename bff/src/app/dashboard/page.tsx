import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {PostResponse} from '@/lib/types/group';
import type {InviteResponse} from '@/lib/types/invite';
import type {MeetingSummaryResponse, TimeChangeProposalResponse} from '@/lib/types/meeting';
import type {UnreadCountResponse} from '@/lib/types/notification';
import type {UserProfileResponse} from '@/lib/types/user';
import WelcomeHeader from '@/app/components/dashboard/home/welcome-header';
import UpcomingMeetings from '@/app/components/dashboard/home/upcoming-meetings';
import RecentPosts from '@/app/components/dashboard/home/recent-posts';
import NeedsAttention from '@/app/components/dashboard/home/needs-attention';
import PendingVotes from '@/app/components/dashboard/home/pending-votes';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/home/home.module.css';

// Each card gets its own data or null, so one failing call degrades that card instead of the page.
function valueOrNull<T>(result: PromiseSettledResult<T>): T | null {
  return result.status === 'fulfilled' ? result.value : null;
}

export default async function DashboardPage() {
  const session = await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<UserProfileResponse>('/api/users/me'),
    get<MeetingSummaryResponse[]>('/api/meetings/upcoming'),
    get<CursorPage<PostResponse>>('/api/posts/recent?size=5'),
    get<CursorPage<InviteResponse>>('/api/invites?status=PENDING&size=5'),
    get<CursorPage<FriendshipResponse>>('/api/users/me/friends/requests/incoming?size=5'),
    get<TimeChangeProposalResponse[]>('/api/meetings/time-changes/pending'),
    get<UnreadCountResponse>('/api/users/me/notifications/unread-count'),
  ] as const);

  if (results.some((result) => result.status === 'rejected' && result.reason instanceof AuthError)) {
    redirect('/login');
  }

  const profile = valueOrNull(results[0]);
  const meetings = valueOrNull(results[1]);
  const posts = valueOrNull(results[2]);
  const invites = valueOrNull(results[3]);
  const friendRequests = valueOrNull(results[4]);
  const votes = valueOrNull(results[5]);
  const unread = valueOrNull(results[6]);
  const timeZone = profile?.timezone ?? 'UTC';

  return (
    <main className={dashboardStyles.page}>
      <WelcomeHeader username={session.username} profile={profile} unread={unread?.unread ?? null}/>
      <PendingVotes proposals={votes} timeZone={timeZone}/>
      <div className={styles.cardGroup}>
        <UpcomingMeetings meetings={meetings} timeZone={timeZone}/>
        <NeedsAttention invites={invites} friendRequests={friendRequests}/>
        <RecentPosts posts={posts}/>
      </div>
    </main>
  );
}
