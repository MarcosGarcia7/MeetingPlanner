import { Style, Avatar } from '@dicebear/core';
import initialsDefinition from '@dicebear/styles/initials.json' with { type: 'json'};
import { authFetch } from '@/lib/api-client';
import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

interface UserProfile {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  avatarColor: string;
  isPrivate: boolean;
  friendsCanViewSchedule: boolean;
  emailVerified: boolean;
}

export async function GET(): Promise<NextResponse> {
    const data = await authFetch<UserProfile>(`${API_BASE_URL}/api/users/me`, {
        method: 'GET',
    });

    if (!data) {
        return new NextResponse(null, { status: 500 });
    }

    const style = new Style(initialsDefinition);

    const name = data.firstName && data.lastName
        ? `${data.firstName} ${data.lastName}`
        : data.username;

    const avatar = new Avatar(style, { seed: name, borderRadius: 50, backgroundColor: [data.avatarColor]});
    const svg = avatar.toString();
    return new NextResponse(svg, {
        headers: { 'Content-Type': 'image/svg+xml' },
    });
}
