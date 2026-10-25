import { apiFetch, getClientIp } from '@/lib/api-client';
import { withErrors } from '@/lib/route-handler';
import { createSession } from '@/lib/session';
import type {
  LoginEmailRequest,
  LoginRequest,
  LoginResponse,
  LoginUsernameRequest,
  TokenResponse,
} from '@/lib/types/auth';

export const POST = withErrors(async (request: Request): Promise<Response> => {
  const { identifier = '', password = '' } = (await request
    .json()
    .catch(() => ({}))) as Partial<LoginRequest>;

  // A missing identifier goes to the username endpoint, whose validation rejects it.
  const isEmail = identifier.includes('@');
  const [endpoint, body] = isEmail
    ? ['/api/auth/login/email', { email: identifier, password } satisfies LoginEmailRequest]
    : ['/api/auth/login/username', { username: identifier, password } satisfies LoginUsernameRequest];

  const tokenResponse = await apiFetch<TokenResponse>(
    endpoint,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    },
    getClientIp(request.headers)
  );

  await createSession({
    userId: tokenResponse.user.id,
    username: tokenResponse.user.username,
    accessToken: tokenResponse.accessToken,
    refreshToken: tokenResponse.refreshToken,
  });

  return Response.json({ user: tokenResponse.user } satisfies LoginResponse);
});
