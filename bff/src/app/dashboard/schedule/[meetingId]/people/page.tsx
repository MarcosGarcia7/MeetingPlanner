import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {GroupResponse} from '@/lib/types/group';
import type {JoinRequestResponse} from '@/lib/types/request';
import {loadMeeting, loadTimeZone} from '@/app/components/dashboard/meetings/load-meeting';
import ParticipantList from '@/app/components/dashboard/meetings/participant-list';
import MeetingInviteForm from '@/app/components/dashboard/meetings/meeting-invite-form';
import MeetingJoinRequests from '@/app/components/dashboard/meetings/meeting-join-requests';

export default async function MeetingPeoplePage({params}: PageProps<'/dashboard/schedule/[meetingId]/people'>) {
  const {userId} = await verifySession();
  const {meetingId} = await params;
  const meeting = await loadMeeting(meetingId);
  if (!meeting.canManage) {
    return <ParticipantList meeting={meeting} viewerId={userId}/>;
  }

  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);
  const results = await Promise.allSettled([
    get<CursorPage<JoinRequestResponse>>(`/api/meetings/${meeting.id}/join-requests?status=PENDING`),
    // Invite suggestions: the group's members for a group meeting, else friends.
    meeting.groupId
      ? get<GroupResponse>(`/api/groups/${meeting.groupId}`)
        .then((group) => (group.members ?? []).flatMap((m) => m.username ? [m.username] : []))
      : get<CursorPage<FriendshipResponse>>('/api/users/me/friends?size=100')
        .then((page) => page.items.map((friend) => friend.username)),
  ] as const);
  if (hasAuthError(results)) {
    redirect('/login');
  }
  const taken = new Set((meeting.participants ?? []).map((p) => p.username?.toLowerCase()));
  const suggestions = (valueOrNull(results[1]) ?? []).filter((username) => !taken.has(username.toLowerCase()));

  return (
    <>
      <ParticipantList meeting={meeting} viewerId={userId}/>
      <MeetingInviteForm meetingId={meeting.id!} suggestions={suggestions}/>
      <MeetingJoinRequests meetingId={meeting.id!} initial={valueOrNull(results[0])} timeZone={await loadTimeZone()}/>
    </>
  );
}
