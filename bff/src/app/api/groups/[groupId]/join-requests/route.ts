import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/groups/[groupId]/join-requests'>
): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/join-requests`);
}

export async function GET(
  request: Request,
  ctx: RouteContext<'/api/groups/[groupId]/join-requests'>
): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/join-requests`);
}
