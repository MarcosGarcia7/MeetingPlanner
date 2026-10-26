import 'server-only';
import {cookies} from 'next/headers';
import {getRedis} from './redis';

const SESSION_PREFIX = 'sess:';
const SESSION_TTL_SECONDS = Number(process.env.SESSION_TTL ?? 1800);
const COOKIE_SECURE =
  process.env.COOKIE_SECURE !== undefined
    ? process.env.COOKIE_SECURE === 'true'
    : process.env.NODE_ENV === 'production';

export const SESSION_COOKIE = 'mp_session';

export const SESSION_COOKIE_OPTIONS = {
  httpOnly: true,
  secure: COOKIE_SECURE,
  sameSite: 'lax',
  path: '/',
  maxAge: SESSION_TTL_SECONDS,
} as const;

export interface SessionData {
  userId: number;
  username: string;
  accessToken: string;
  refreshToken: string;
}

export async function createSession(data: SessionData): Promise<string> {
  const sessionId = crypto.randomUUID();
  await getRedis().set(
    `${SESSION_PREFIX}${sessionId}`,
    JSON.stringify(data),
    'EX',
    SESSION_TTL_SECONDS
  );

  const cookieStore = await cookies();
  cookieStore.set(SESSION_COOKIE, sessionId, SESSION_COOKIE_OPTIONS);

  return sessionId;
}

export async function getSession(): Promise<SessionData | null> {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(SESSION_COOKIE)?.value;
  if (!sessionId) return null;

  const raw = await getRedis().get(`${SESSION_PREFIX}${sessionId}`);
  if (!raw) return null;

  return JSON.parse(raw) as SessionData;
}

export async function getSessionId(): Promise<string | null> {
  const cookieStore = await cookies();
  return cookieStore.get(SESSION_COOKIE)?.value ?? null;
}

export async function destroySession(): Promise<void> {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(SESSION_COOKIE)?.value;

  if (sessionId) {
    await getRedis().del(`${SESSION_PREFIX}${sessionId}`);
  }

  cookieStore.delete(SESSION_COOKIE);
}

// Extends the Redis TTL only; the caller re-sets the cookie (proxy does it on its response). Returns
// false if the session no longer exists, so an expired session is never revived.
export async function refreshSessionTtl(sessionId: string): Promise<boolean> {
  const updated = await getRedis().expire(`${SESSION_PREFIX}${sessionId}`, SESSION_TTL_SECONDS);
  return updated === 1;
}

export async function updateSession(sessionId: string, data: SessionData): Promise<void> {
  await getRedis().set(
    `${SESSION_PREFIX}${sessionId}`,
    JSON.stringify(data),
    'EX',
    SESSION_TTL_SECONDS
  );
}
