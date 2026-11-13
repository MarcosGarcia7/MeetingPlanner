import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/invite-links/meeting/[token]/accept'>
): Promise<Response> {
  const {token} = await ctx.params;
  return forward(request, `/api/invite-links/meeting/${encodeURIComponent(token)}/accept`);
}
