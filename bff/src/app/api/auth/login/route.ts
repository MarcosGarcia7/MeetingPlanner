import { NextResponse } from 'next/server';
import { getClientIp } from '@/lib/api-client';
import { createSession } from '@/lib/session';

const API_BASE_URL =
  process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

export async function POST(request: Request): Promise<NextResponse> {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  const clientIp = getClientIp(request.headers);
  if (clientIp) {
    headers.set('X-Forwarded-For', clientIp);
  }

  const formData = await request.formData();
  const identifier = String(formData.get('identifier') ?? '');
  const password = String(formData.get('password') ?? '');

  if (!identifier || !password) {
    return NextResponse.json(
      { error: 'validation_failed', message: 'email or username and password are required' },
      { status: 400 }
    );
  }

  const isEmail = identifier.includes('@');
  const endpoint = isEmail ? '/api/auth/login/email' : '/api/auth/login/username';
  const body = isEmail ? { email: identifier } : { username: identifier };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ...body, password }),
  });

  if (!response.ok) {
    let responseBody: unknown = null;
    try {
      responseBody = await response.json();
    } catch {
      // ignore body parse errors
    }
    return NextResponse.json(
      responseBody ?? { error: 'login_failed', message: 'Login failed' },
      { status: response.status }
    );
  }

  const tokenResponse = await response.json();
  await createSession({
    userId: tokenResponse.user.id,
    username: tokenResponse.user.username,
    accessToken: tokenResponse.accessToken,
    refreshToken: tokenResponse.refreshToken,
  });

  return NextResponse.json({ user: tokenResponse.user });
}