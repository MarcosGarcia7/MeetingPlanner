import {forward} from '@/lib/route-handler';

export async function GET(request: Request,
                          ctx: RouteContext<'/api/meetings/[meetingId]'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}`);
}

export async function PATCH(request: Request,
                            ctx: RouteContext<'/api/meetings/[meetingId]'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}`);
}

export async function DELETE(request: Request,
                             ctx: RouteContext<'/api/meetings/[meetingId]'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}`);
}
