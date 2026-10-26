import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/invites/group/[inviteId]/decline'>
): Promise<Response> {
  const {inviteId} = await ctx.params;
  return forward(request, `/api/invites/group/${encodeURIComponent(inviteId)}/decline`);
}
