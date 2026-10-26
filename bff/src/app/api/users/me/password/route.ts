import {authFetch, getClientIp} from '@/lib/api-client';
import {withErrors} from '@/lib/route-handler';
import {getSession, getSessionId, updateSession} from '@/lib/session';
import type {TokenResponse} from '@/lib/types/auth';

// The API revokes every refresh token (this session's included) and returns a fresh pair, so store it
// in the session instead of passing it through; the browser only gets a 204.
export const POST = withErrors(async (request: Request): Promise<Response> => {
  const tokens = await authFetch<TokenResponse>(
    '/api/users/me/password',
    {method: 'POST', headers: {'Content-Type': 'application/json'}, body: await request.text()},
    getClientIp(request.headers)
  );

  const [session, sessionId] = await Promise.all([getSession(), getSessionId()]);
  if (session && sessionId) {
    await updateSession(sessionId, {...session, accessToken: tokens.accessToken, refreshToken: tokens.refreshToken});
  }
  return new Response(null, {status: 204});
});
