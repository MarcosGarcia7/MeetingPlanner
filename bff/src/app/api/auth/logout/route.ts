import { NextResponse } from 'next/server';
import { API_BASE_URL, getClientIp } from '@/lib/api-client';
import { destroySession, getSession } from '@/lib/session';
import type { LogoutRequest } from '@/lib/types/auth';

export async function POST(request: Request): Promise<NextResponse> {
  const session = await getSession();

  if (session?.refreshToken) {
    const headers = new Headers({ 'Content-Type': 'application/json' });
    const clientIp = getClientIp(request.headers);
    if (clientIp) {
      headers.set('X-Forwarded-For', clientIp);
    }

    try {
      await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          refreshToken: session.refreshToken,
          allSessions: false,
        } satisfies LogoutRequest),
      });
    } catch {
      // best-effort revocation; local session is cleared regardless
    }
  }

  await destroySession();
  return new NextResponse(null, { status: 204 });
}
