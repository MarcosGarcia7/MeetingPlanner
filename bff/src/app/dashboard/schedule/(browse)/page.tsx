import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {ApiError, authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {InviteResponse} from '@/lib/types/invite';
import type {MeetingSummaryResponse, TimeChangeProposalResponse} from '@/lib/types/meeting';
import type {JoinRequestResponse} from '@/lib/types/request';
import type {UserAvailabilityResponse, UserProfileResponse} from '@/lib/types/user';
import PendingVotes from '@/app/components/dashboard/home/pending-votes';
import MeetingListItem from '@/app/components/dashboard/home/meeting-list-item';
import PendingMeetingItems from '@/app/components/dashboard/schedule/pending-meeting-items';
import AvailabilitySummary from '@/app/components/dashboard/schedule/availability-summary';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import styles from '@/app/components/dashboard/schedule/schedule.module.css';

const PREVIEW_SIZE = 5;

export default async function ScheduleOverviewPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<TimeChangeProposalResponse[]>('/api/meetings/time-changes/pending'),
    get<CursorPage<InviteResponse>>('/api/invites?status=PENDING&kind=MEETING&size=50'),
    get<CursorPage<JoinRequestResponse>>('/api/requests?status=PENDING&kind=MEETING&size=50'),
    get<CursorPage<MeetingSummaryResponse>>(`/api/meetings?when=UPCOMING&size=${PREVIEW_SIZE}`),
    get<UserAvailabilityResponse>('/api/users/me/availability'),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const timeZone = valueOrNull(results[5])?.timezone ?? 'UTC';
  const invites = valueOrNull(results[1])?.items ?? [];
  const requests = valueOrNull(results[2])?.items ?? [];
  const upcoming = valueOrNull(results[3]);
  // 404: availability was never saved (null); any other failure leaves it undefined.
  const availabilityResult = results[4];
  const availability = availabilityResult.status === 'fulfilled' ? availabilityResult.value
    : availabilityResult.reason instanceof ApiError && availabilityResult.reason.status === 404 ? null : undefined;

  return (
    <>
      <PendingVotes proposals={valueOrNull(results[0])} timeZone={timeZone}/>
      {(invites.length > 0 || requests.length > 0) && <PendingMeetingItems invites={invites} requests={requests}/>}
      <div className={styles.shelves}>
        <section className={dashboardStyles.card}>
          <div className={dashboardStyles.cardHeader}>
            <h2 className={dashboardStyles.cardTitle}>Coming up</h2>
          </div>
          {upcoming === null ? (
            <p className={dashboardStyles.error}>Couldn’t load your meetings.</p>
          ) : upcoming.items.length === 0 ? (
            <p className={dashboardStyles.empty}>Nothing scheduled.</p>
          ) : (
            <ul className={homeStyles.cardList}>
              {upcoming.items.map((meeting) => <MeetingListItem key={meeting.id} meeting={meeting} timeZone={timeZone}/>)}
            </ul>
          )}
          <div className={dashboardStyles.cardFooter}>
            <div className={dashboardStyles.actions}>
              {upcoming?.nextCursor && (
                <Link href='/dashboard/schedule/meetings' className={dashboardStyles.cardLink}>See all</Link>
              )}
              <Link href='/dashboard/schedule/meetings?new=1' className={dashboardStyles.cardLink}>New meeting</Link>
            </div>
          </div>
        </section>
        <AvailabilitySummary availability={availability} timeZone={timeZone}/>
      </div>
    </>
  );
}
