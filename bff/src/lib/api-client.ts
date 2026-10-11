import { getSession, getSessionId, destroySession, updateSession, type SessionData } from './session';

export const API_BASE_URL =
  process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message);
  }
}

export class AuthError extends Error {}

export function getClientIp(headers: Headers): string | undefined {
  const forwarded = headers.get('x-forwarded-for');
  if (!forwarded) return undefined;

  const first = forwarded.split(',')[0].trim().replace(/[\r\n]/g, '');
  return first || undefined;
}

async function refreshAccessToken(session: SessionData): Promise<TokenResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  });

  if (!res.ok) {
    throw new AuthError();
  }

  return (await res.json()) as TokenResponse;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

function addAuthHeader(headers: Headers, session: SessionData | null): void {
  if (session) {
    headers.set('Authorization', `Bearer ${session.accessToken}`);
  }
}

async function requestWithAuth<T>(
  path: string,
  init: RequestInit = {},
  clientIp?: string
): Promise<T> {
  let session = await getSession();
  const headers = new Headers(init.headers);
  addAuthHeader(headers, session);
  if (clientIp) {
    headers.set('X-Forwarded-For', clientIp);
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (res.ok) {
    if (res.status === 204) {
      return undefined as T;
    }
    return (await res.json()) as T;
  }

  if (res.status === 401 && session) {
    try {
      const tokenResponse = await refreshAccessToken(session);
      const sessionId = await getSessionId();
      if (sessionId) {
        await updateSession(sessionId, {
          ...session,
          accessToken: tokenResponse.accessToken,
          refreshToken: tokenResponse.refreshToken,
        });
      }
      session = { ...session, accessToken: tokenResponse.accessToken, refreshToken: tokenResponse.refreshToken };

      const retryHeaders = new Headers(init.headers);
      addAuthHeader(retryHeaders, session);
      if (clientIp) {
        retryHeaders.set('X-Forwarded-For', clientIp);
      }

      const retryRes = await fetch(`${API_BASE_URL}${path}`, { ...init, headers: retryHeaders });

      if (!retryRes.ok) {
        throw new ApiError(retryRes.status, `API ${path} failed with ${retryRes.status}`);
      }

      if (retryRes.status === 204) {
        return undefined as T;
      }
      return (await retryRes.json()) as T;
    } catch (err) {
      if (err instanceof AuthError) {
        await destroySession();
        throw new AuthError('Session expired. Please log in again.');
      }
      throw err;
    }
  }

  throw new ApiError(res.status, `API ${path} failed with ${res.status}`);
}

export async function authFetch<T>(
  path: string,
  init?: RequestInit,
  clientIp?: string
): Promise<T> {
  return requestWithAuth<T>(path, init, clientIp);
}
