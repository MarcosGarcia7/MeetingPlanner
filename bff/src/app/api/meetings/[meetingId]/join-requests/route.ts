import {forward} from '@/lib/route-handler';

export async function GET(request: Request,
                          ctx: RouteContext<'/api/meetings/[meetingId]/join-requests'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/join-requests`);
}

export async function POST(request: Request,
                           ctx: RouteContext<'/api/meetings/[meetingId]/join-requests'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/join-requests`);
}
