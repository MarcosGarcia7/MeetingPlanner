import {forward} from '@/lib/route-handler';

export async function GET(request: Request, ctx: RouteContext<'/api/groups/[groupId]/meetings'>): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/meetings`);
}

export async function POST(request: Request, ctx: RouteContext<'/api/groups/[groupId]/meetings'>): Promise<Response> {
  const {groupId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/meetings`);
}
