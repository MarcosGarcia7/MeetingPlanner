import { Style, Avatar } from '@dicebear/core';
import initialsDefinition from '@dicebear/styles/initials.json' with { type: 'json'};
import { authFetch, getClientIp } from '@/lib/api-client';
import { withErrors } from '@/lib/route-handler';
import type { UserProfileResponse } from '@/lib/types/user';

export const GET = withErrors(async (request: Request): Promise<Response> => {
    const data = await authFetch<UserProfileResponse>('/api/users/me', {}, getClientIp(request.headers));

    const style = new Style(initialsDefinition);

    const name = data.firstName && data.lastName
        ? `${data.firstName} ${data.lastName}`
        : data.username;

    const avatar = new Avatar(style, { seed: name, borderRadius: 50, backgroundColor: [data.avatarColor]});
    const svg = avatar.toString();
    return new Response(svg, {
        headers: { 'Content-Type': 'image/svg+xml' },
    });
});
