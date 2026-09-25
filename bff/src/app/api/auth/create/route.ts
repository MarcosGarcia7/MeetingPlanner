import { NextResponse } from 'next/server';
import { getClientIp } from '@/lib/api-client';

const API_BASE_URL =
  process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

export async function POST(request: Request): Promise<NextResponse> {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  const clientIp = getClientIp(request.headers);
  if (clientIp) {
    headers.set('X-Forwarded-For', clientIp);
  }

  const formData = await request.formData();
  const email = formData.get('email');
  const username = formData.get('username');
  const password = formData.get('password');
  const firstName = formData.get('firstName');
  const lastName = formData.get('lastName');
  const isPrivate = formData.get('isPrivate') === 'true';

  if (!email || !password) {
    return NextResponse.json(
      { error: 'validation_failed', message: 'email and password are required' },
      { status: 400 }
    );
  }

  const response = await fetch(`${API_BASE_URL}/api/auth/create`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      email,
      username: username || null,
      password,
      firstName: firstName || null,
      lastName: lastName || null,
      isPrivate,
    }),
  });

  if (!response.ok) {
    let body: unknown = null;
    try {
      body = await response.json();
    } catch {
      // ignore body parse errors
    }
    return NextResponse.json(
      body ?? { error: 'signup_failed', message: 'Sign up failed' },
      { status: response.status }
    );
  }

  const body = await response.json();
  return NextResponse.json(body, { status: response.status });
}