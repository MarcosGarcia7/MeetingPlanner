import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {GroupSummaryResponse} from '@/lib/types/group';
import type {MeetingSummaryResponse} from '@/lib/types/meeting';
import type {UserProfileResponse} from '@/lib/types/user';
import ScheduleMeetings from '@/app/components/dashboard/schedule/schedule-meetings';
import {listUrl, type MeetingsView} from '@/app/components/dashboard/schedule/meetings-views';

// Groups offered in the form and friends suggested as invitees; one page of each is plenty.
const OPTIONS_SIZE = 100;

function toView(value: string | string[] | undefined): MeetingsView {
  return value === 'past' || value === 'created' ? value : 'upcoming';
}

export default async function ScheduleMeetingsPage({searchParams}: PageProps<'/dashboard/schedule/meetings'>) {
  const {userId} = await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);
  const {view: viewParam, new: creating} = await searchParams;
  const view = toView(viewParam);

  const results = await Promise.allSettled([
    get<CursorPage<MeetingSummaryResponse>>(listUrl(view)),
    get<UserProfileResponse>('/api/users/me'),
    get<CursorPage<GroupSummaryResponse>>(`/api/groups?size=${OPTIONS_SIZE}`),
    get<CursorPage<FriendshipResponse>>(`/api/users/me/friends?size=${OPTIONS_SIZE}`),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  const groups = (valueOrNull(results[2])?.items ?? [])
    .filter((group) => group.canCreateMeetings)
    .map((group) => ({id: group.id, name: group.groupName ?? 'Untitled group'}));
  const friends = (valueOrNull(results[3])?.items ?? []).map((friend) => friend.username);

  // Keyed by view so switching filters starts a fresh list instead of keeping the last one's pages.
  return (
    <ScheduleMeetings key={view} view={view} initial={valueOrNull(results[0])}
                      timeZone={valueOrNull(results[1])?.timezone ?? 'UTC'} groups={groups} friendUsernames={friends}
                      viewerId={userId} creating={creating === '1'}/>
  );
}
