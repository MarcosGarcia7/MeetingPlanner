import {forward} from '@/lib/route-handler';

type Ctx = RouteContext<'/api/groups/[groupId]/share-link'>;

async function apiPath(ctx: Ctx): Promise<string> {
  const {groupId} = await ctx.params;
  return `/api/groups/${encodeURIComponent(groupId)}/share-link`;
}

export async function GET(request: Request, ctx: Ctx): Promise<Response> {
  return forward(request, await apiPath(ctx));
}

export async function POST(request: Request, ctx: Ctx): Promise<Response> {
  return forward(request, await apiPath(ctx));
}

export async function DELETE(request: Request, ctx: Ctx): Promise<Response> {
  return forward(request, await apiPath(ctx));
}
