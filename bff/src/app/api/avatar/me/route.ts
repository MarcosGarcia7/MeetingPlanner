import {authFetch, getClientIp} from '@/lib/api-client';
import {avatarSvg} from '@/lib/avatar';
import {withErrors} from '@/lib/route-handler';
import type {UserProfileResponse} from '@/lib/types/user';

export const GET = withErrors(async (request: Request): Promise<Response> => {
  const data = await authFetch<UserProfileResponse>('/api/users/me', {}, getClientIp(request.headers));

  return new Response(avatarSvg(data), {
    // Per-user image behind a shared URL: never let a browser or proxy cache keep it.
    headers: {'Content-Type': 'image/svg+xml', 'Cache-Control': 'private, no-store'},
  });
});
