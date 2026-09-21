import { cookies } from 'next/headers';
import { getRedis } from './redis';

const SESSION_PREFIX = 'sess:';
const SESSION_TTL_SECONDS = Number(process.env.SESSION_TTL ?? 1800);
const COOKIE_SECURE =
  process.env.COOKIE_SECURE !== undefined
    ? process.env.COOKIE_SECURE === 'true'
    : process.env.NODE_ENV === 'production';

export const SESSION_COOKIE = 'mp_session';

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
  cookieStore.set(SESSION_COOKIE, sessionId, {
    httpOnly: true,
    secure: COOKIE_SECURE,
    sameSite: 'lax',
    path: '/',
    maxAge: SESSION_TTL_SECONDS,
  });

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

export async function destroySession(): Promise<void> {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(SESSION_COOKIE)?.value;

  if (sessionId) {
    await getRedis().del(`${SESSION_PREFIX}${sessionId}`);
  }

  cookieStore.delete(SESSION_COOKIE);
}

export async function refreshSessionTtl(): Promise<void> {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(SESSION_COOKIE)?.value;
  if (!sessionId) return;

  await getRedis().expire(`${SESSION_PREFIX}${sessionId}`, SESSION_TTL_SECONDS);
}
