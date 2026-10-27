import {forward} from '@/lib/route-handler';

type Context = RouteContext<'/api/groups/[groupId]/bans/[userId]'>;

export async function POST(request: Request, ctx: Context): Promise<Response> {
  const {groupId, userId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/bans/${encodeURIComponent(userId)}`);
}

export async function DELETE(request: Request, ctx: Context): Promise<Response> {
  const {groupId, userId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/bans/${encodeURIComponent(userId)}`);
}
