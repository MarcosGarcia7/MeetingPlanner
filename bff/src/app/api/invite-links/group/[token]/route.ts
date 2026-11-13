import {forward} from '@/lib/route-handler';

export async function GET(
  request: Request,
  ctx: RouteContext<'/api/invite-links/group/[token]'>
): Promise<Response> {
  const {token} = await ctx.params;
  return forward(request, `/api/invite-links/group/${encodeURIComponent(token)}`);
}
