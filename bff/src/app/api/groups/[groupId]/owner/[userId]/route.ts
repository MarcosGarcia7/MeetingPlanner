import {forward} from '@/lib/route-handler';

export async function PUT(request: Request,
                          ctx: RouteContext<'/api/groups/[groupId]/owner/[userId]'>): Promise<Response> {
  const {groupId, userId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/owner/${encodeURIComponent(userId)}`);
}
