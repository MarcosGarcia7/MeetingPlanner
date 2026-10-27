import {forward} from '@/lib/route-handler';

export async function PATCH(request: Request, ctx: RouteContext<'/api/groups/[groupId]'>): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}`);
}

export async function DELETE(request: Request, ctx: RouteContext<'/api/groups/[groupId]'>): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}`);
}
