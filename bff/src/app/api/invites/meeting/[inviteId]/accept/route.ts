import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/invites/meeting/[inviteId]/accept'>
): Promise<Response> {
  const {inviteId} = await ctx.params;
  return forward(request, `/api/invites/meeting/${encodeURIComponent(inviteId)}/accept`);
}
