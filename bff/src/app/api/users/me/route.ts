import {authFetch, getClientIp} from '@/lib/api-client';
import {forward, withErrors} from '@/lib/route-handler';
import {destroySession} from '@/lib/session';

export async function GET(request: Request): Promise<Response> {
  return forward(request, '/api/users/me');
}

export async function PATCH(request: Request): Promise<Response> {
  return forward(request, '/api/users/me');
}

// The API revokes every refresh token along with the account, so the BFF session goes too.
export const DELETE = withErrors(async (request: Request): Promise<Response> => {
  await authFetch<void>(
    '/api/users/me',
    {method: 'DELETE', headers: {'Content-Type': 'application/json'}, body: await request.text()},
    getClientIp(request.headers)
  );
  await destroySession();
  return new Response(null, {status: 204});
});
