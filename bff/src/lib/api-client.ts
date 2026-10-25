import 'server-only';
import {getSession, getSessionId, destroySession, updateSession, type SessionData} from './session';
import type {RefreshRequest, TokenResponse} from './types/auth';

export const API_BASE_URL =
  process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    // The API's parsed error body ({ error, message, ... }), or null if it wasn't JSON.
    public readonly body: unknown = null,
    public readonly retryAfter?: string
  ) {
    super(message);
  }
}

export class AuthError extends Error {
}

export function getClientIp(headers: Headers): string | undefined {
  const forwarded = headers.get('x-forwarded-for');
  if (!forwarded) return undefined;

  const first = forwarded.split(',')[0].trim().replace(/[\r\n]/g, '');
  return first || undefined;
}

async function refreshAccessToken(session: SessionData): Promise<TokenResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({refreshToken: session.refreshToken} satisfies RefreshRequest),
  });

  if (!res.ok) {
    throw new AuthError();
  }

  return (await res.json()) as TokenResponse;
}

function buildHeaders(init: RequestInit, session: SessionData | null, clientIp?: string): Headers {
  const headers = new Headers(init.headers);
  if (session) {
    headers.set('Authorization', `Bearer ${session.accessToken}`);
  }
  if (clientIp) {
    headers.set('X-Forwarded-For', clientIp);
  }
  return headers;
}

// Sends an authenticated request and returns the API's response as-is (including error statuses).
// Refreshes once on 401; throws AuthError and destroys the session if the refresh fails.
export async function authFetchRaw(
  path: string,
  init: RequestInit = {},
  clientIp?: string
): Promise<Response> {
  const session = await getSession();
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: buildHeaders(init, session, clientIp),
  });

  if (res.status !== 401 || !session) {
    return res;
  }

  let tokenResponse: TokenResponse;
  try {
    tokenResponse = await refreshAccessToken(session);
  } catch (err) {
    if (err instanceof AuthError) {
      await destroySession();
      throw new AuthError('Session expired. Please log in again.');
    }
    throw err;
  }

  const refreshed = {
    ...session,
    accessToken: tokenResponse.accessToken,
    refreshToken: tokenResponse.refreshToken,
  };
  const sessionId = await getSessionId();
  if (sessionId) {
    await updateSession(sessionId, refreshed);
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: buildHeaders(init, refreshed, clientIp),
  });
}

// Parses a successful API response as T (undefined for 204); throws ApiError otherwise.
async function parseResponse<T>(res: Response, path: string): Promise<T> {
  if (!res.ok) {
    let body: unknown = null;
    try {
      body = await res.json();
    } catch {
      // ignore body parse errors
    }
    throw new ApiError(
      res.status,
      `API ${path} failed with ${res.status}`,
      body,
      res.headers.get('retry-after') ?? undefined
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export async function authFetch<T>(
  path: string,
  init?: RequestInit,
  clientIp?: string
): Promise<T> {
  const res = await authFetchRaw(path, init, clientIp);
  return parseResponse<T>(res, path);
}

// Unauthenticated counterpart of authFetch, for auth endpoints called before a session exists.
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  clientIp?: string
): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: buildHeaders(init, null, clientIp),
  });
  return parseResponse<T>(res, path);
}
