import {forward} from '@/lib/route-handler';

type Ctx = RouteContext<'/api/meetings/[meetingId]/share-link'>;

async function apiPath(ctx: Ctx): Promise<string> {
  const {meetingId} = await ctx.params;
  return `/api/meetings/${encodeURIComponent(meetingId)}/share-link`;
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
