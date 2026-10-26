import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/users/me/friends/requests/[username]/accept'>
): Promise<Response> {
  const {username} = await ctx.params;
  return forward(request, `/api/users/me/friends/requests/${encodeURIComponent(username)}/accept`);
}
