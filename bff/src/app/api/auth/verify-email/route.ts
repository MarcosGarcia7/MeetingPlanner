import { NextResponse } from 'next/server';

const API_BASE_URL =
  process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

export async function POST(request: Request): Promise<NextResponse> {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  const clientIp = request.headers.get('x-forwarded-for');
  if (clientIp) {
    headers.set('X-Forwarded-For', clientIp);
  }

  let token: string | null = null;
  try {
    const json = await request.json();
    token = typeof json.token === 'string' ? json.token : null;
  } catch {
    // body parse errors handled below
  }

  if (!token) {
    return NextResponse.json(
      { error: 'validation_failed', message: 'token is required' },
      { status: 400 }
    );
  }

  const response = await fetch(`${API_BASE_URL}/api/auth/verify-email`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ token }),
  });

  if (!response.ok) {
    let body: unknown = null;
    try {
      body = await response.json();
    } catch {
      // ignore body parse errors
    }
    return NextResponse.json(
      body ?? { error: 'verification_failed', message: 'Verification failed' },
      { status: response.status }
    );
  }

  return NextResponse.json({ verified: true });
}