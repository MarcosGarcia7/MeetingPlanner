import {forward} from '@/lib/route-handler';

export async function DELETE(
  request: Request,
  ctx: RouteContext<'/api/users/me/friends/blocked/[username]'>
): Promise<Response> {
  const {username} = await ctx.params;
  return forward(request, `/api/users/me/friends/blocked/${encodeURIComponent(username)}`);
}
