import {forward} from '@/lib/route-handler';

export async function POST(request: Request, ctx: RouteContext<'/api/groups/[groupId]/invites'>): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/invites`);
}
