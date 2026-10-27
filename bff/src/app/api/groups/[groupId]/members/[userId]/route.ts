import {forward} from '@/lib/route-handler';

type Context = RouteContext<'/api/groups/[groupId]/members/[userId]'>;

// Toggles admin.
export async function PATCH(request: Request, ctx: Context): Promise<Response> {
  const {groupId, userId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(userId)}`);
}

export async function DELETE(request: Request, ctx: Context): Promise<Response> {
  const {groupId, userId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(userId)}`);
}
