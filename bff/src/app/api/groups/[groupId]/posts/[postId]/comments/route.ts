import {forward} from '@/lib/route-handler';

type Context = RouteContext<'/api/groups/[groupId]/posts/[postId]/comments'>;

async function apiPath(ctx: Context) {
  const {groupId, postId} = await ctx.params;
  return `/api/groups/${encodeURIComponent(groupId)}/posts/${encodeURIComponent(postId)}/comments`;
}

export async function GET(request: Request, ctx: Context): Promise<Response> {
  return forward(request, await apiPath(ctx));
}

export async function POST(request: Request, ctx: Context): Promise<Response> {
  return forward(request, await apiPath(ctx));
}

export async function PATCH(request: Request, ctx: Context): Promise<Response> {
  return forward(request, await apiPath(ctx));
}
