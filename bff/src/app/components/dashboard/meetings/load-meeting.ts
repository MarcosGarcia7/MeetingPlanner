import 'server-only';
import {cache} from 'react';
import {headers} from 'next/headers';
import {notFound, redirect} from 'next/navigation';
import {ApiError, AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {MeetingResponse} from '@/lib/types/meeting';
import type {UserProfileResponse} from '@/lib/types/user';

// The meeting for /dashboard/schedule/[meetingId]. Cached per request, so the layout and the tab page
// share one API call. Unknown or malformed ids, and meetings the viewer can't see, render the 404 page.
export const loadMeeting = cache(async (meetingId: string): Promise<MeetingResponse> => {
  try {
    return await authFetch<MeetingResponse>(`/api/meetings/${encodeURIComponent(meetingId)}`, {},
      getClientIp(await headers()));
  } catch (error) {
    if (error instanceof AuthError) {
      redirect('/login');
    }
    if (error instanceof ApiError && (error.status === 400 || error.status === 404)) {
      notFound();
    }
    throw error;
  }
});

// The viewer's profile timezone ('UTC' when unset or unavailable), shared by the layout and tab pages.
export const loadTimeZone = cache(async (): Promise<string> => {
  try {
    const profile = await authFetch<UserProfileResponse>('/api/users/me', {}, getClientIp(await headers()));
    return profile.timezone ?? 'UTC';
  } catch (error) {
    if (error instanceof AuthError) {
      redirect('/login');
    }
    return 'UTC';
  }
});

// The organizer's username, from the participant list (the creator is always a participant).
export function organizerOf(meeting: MeetingResponse): string | null {
  return meeting.participants?.find((p) => p.userId === meeting.creatorId)?.username ?? null;
}
