import { NextResponse } from 'next/server';
import { authFetch } from '@/lib/api-client';

export async function GET(request: Request): Promise<NextResponse> {
  try {
    const data = await authFetch<{ days: Array<{ dayOfWeek: number; startTime: string; endTime: string }> }>(
      '/api/users/me/availability'
    );
    return NextResponse.json(data);
  } catch (err) {
    const status = err instanceof Error && 'status' in err ? (err as any).status : 500;
    return NextResponse.json({ error: 'Failed to fetch availability' }, { status });
  }
}

export async function PUT(request: Request): Promise<NextResponse> {
  try {
    const body = await request.json();
    const data = await authFetch('/api/users/me/availability', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return NextResponse.json(data);
  } catch (err) {
    const status = err instanceof Error && 'status' in err ? (err as any).status : 500;
    return NextResponse.json({ error: 'Failed to update availability' }, { status });
  }
}
