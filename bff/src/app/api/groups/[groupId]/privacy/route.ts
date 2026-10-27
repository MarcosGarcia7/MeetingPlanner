import {forward} from '@/lib/route-handler';

export async function PATCH(request: Request, ctx: RouteContext<'/api/groups/[groupId]/privacy'>): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/privacy`);
}
